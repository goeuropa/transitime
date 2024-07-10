/* (C)2023 */
package org.transitclock.service;

import lombok.extern.slf4j.Slf4j;
import org.transitclock.Core;
import org.transitclock.core.dataCache.VehicleDataCache;
import org.transitclock.domain.structs.*;
import org.transitclock.gtfs.DbConfig;
import org.transitclock.service.contract.ConfigInterface;
import org.transitclock.service.dto.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements ConfigInterface to serve up configuration information to RMI clients.
 *
 * @author SkiBu Smith
 */
@Slf4j
public class ConfigServiceImpl implements ConfigInterface {

    // Should only be accessed as singleton class
    private static ConfigServiceImpl singleton;


    public static ConfigInterface instance() {
        return singleton;
    }

    /**
     * Starts up the ConfigServer so that RMI calls can query for configuration data. This will
     * automatically cause the object to continue to run and serve requests.
     *
     * @return the singleton ConfigServer object. Usually does not need to used since the server
     *     will be fully running.
     */
    public static ConfigServiceImpl start() {
        if (singleton == null) {
            singleton = new ConfigServiceImpl();
        }

        return singleton;
    }

    public ConfigServiceImpl() {
    }

    /**
     * For getting route from routeIdOrShortName. Tries using routeIdOrShortName as first a route
     * short name to see if there is such a route. If not, then uses routeIdOrShortName as a
     * routeId.
     *
     * @param routeIdOrShortName
     * @return The Route, or null if no such route
     */
    private Route getRoute(String routeIdOrShortName) {
        DbConfig dbConfig = Core.getInstance().getDbConfig();
        Route dbRoute = dbConfig.getRouteByShortName(routeIdOrShortName);
        if (dbRoute == null) {
            dbRoute = dbConfig.getRouteById(routeIdOrShortName);
        }

        return dbRoute;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getRoutes()
     */
    @Override
    public Collection<IpcRouteSummary> getRoutes() {
        // Get the db route info
        DbConfig dbConfig = Core.getInstance().getDbConfig();
        var dbRoutes = dbConfig.getRoutes();

        return dbRoutes
                .stream()
                .map(IpcRouteSummary::new)
                .collect(Collectors.toList());
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getRoute(java.lang.String)
     */
    @Override
    public IpcRoute getRoute(String routeIdOrShortName, String directionId, String stopId, String tripPatternId)
            {
        // Determine the route
        Route dbRoute = getRoute(routeIdOrShortName);
        if (dbRoute == null) {
            return null;
        }

        // Convert db route into an ipc route and return it
        return new IpcRoute(dbRoute, directionId, stopId, tripPatternId);
    }

    /*
     * (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getRoutes(java.util.List)
     */
    @Override
    public List<IpcRoute> getRoutes(List<String> routeIdsOrShortNames) {
        List<IpcRoute> routes = new ArrayList<>();

        // If no route specified then return data for all routes
        if (routeIdsOrShortNames == null || routeIdsOrShortNames.isEmpty()) {
            DbConfig dbConfig = Core.getInstance().getDbConfig();
            List<Route> dbRoutes = dbConfig.getRoutes();
            for (Route dbRoute : dbRoutes) {
                IpcRoute ipcRoute = new IpcRoute(dbRoute, null, null, null);
                routes.add(ipcRoute);
            }
        } else {
            // Routes specified so return data for those routes
            for (String routeIdOrShortName : routeIdsOrShortNames) {
                // Determine the route
                Route dbRoute = getRoute(routeIdOrShortName);
                if (dbRoute == null) continue;

                IpcRoute ipcRoute = new IpcRoute(dbRoute, null, null, null);
                routes.add(ipcRoute);
            }
        }

        return routes;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getStops(java.lang.String)
     */
    @Override
    public IpcDirectionsForRoute getStops(String routeIdOrShortName) {
        // Get the db route info
        Route dbRoute = getRoute(routeIdOrShortName);
        if (dbRoute == null) return null;

        // Return the ipc route
        return new IpcDirectionsForRoute(dbRoute);
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getBlock(java.lang.String, java.lang.String)
     */
    @Override
    public IpcBlock getBlock(String blockId, String serviceId) {
        Block dbBlock = Core.getInstance().getDbConfig().getBlock(serviceId, blockId);

        // If no such block then return null since can't create a IpcBlock
        if (dbBlock == null) {
            return null;
        }

        return new IpcBlock(dbBlock);
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getBlocks(java.lang.String)
     */
    @Override
    public Collection<IpcBlock> getBlocks(String blockId) {
        // For returning results
        List<IpcBlock> ipcBlocks = new ArrayList<>();

        // Get the blocks with specified ID
        Collection<Block> dbBlocks = Core.getInstance().getDbConfig().getBlocksForAllServiceIds(blockId);

        // Convert blocks from DB into IpcBlocks
        for (Block dbBlock : dbBlocks) {
            ipcBlocks.add(new IpcBlock(dbBlock));
        }

        // Return result
        return ipcBlocks;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getTrip(java.lang.String)
     */
    @Override
    public IpcTrip getTrip(String tripId) {
        Trip dbTrip = Core.getInstance().getDbConfig().getTrip(tripId);

        // If couldn't find a trip with the specified trip_id then see if a
        // trip has the trip_short_name specified.
        if (dbTrip == null) {
            dbTrip = Core.getInstance().getDbConfig().getTripUsingTripShortName(tripId);
        }

        // If no such trip then return null since can't create a IpcTrip
        if (dbTrip == null) {
            return null;
        }

        return new IpcTrip(dbTrip);
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getTripPattern(java.lang.String)
     */
    @Override
    public List<IpcTripPattern> getTripPatterns(String routeIdOrShortName) {
        DbConfig dbConfig = Core.getInstance().getDbConfig();

        Route dbRoute = getRoute(routeIdOrShortName);
        if (dbRoute == null) return null;

        List<TripPattern> dbTripPatterns = dbConfig.getTripPatternsForRoute(dbRoute.getId());
        if (dbTripPatterns == null) return null;

        List<IpcTripPattern> tripPatterns = new ArrayList<>();
        for (TripPattern dbTripPattern : dbTripPatterns) {
            tripPatterns.add(new IpcTripPattern(dbTripPattern));
        }
        return tripPatterns;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getAgencies()
     */
    @Override
    public List<Agency> getAgencies() {
        return Core.getInstance().getDbConfig().getAgencies();
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getSchedules(java.lang.String)
     */
    @Override
    public List<IpcSchedule> getSchedules(String routeIdOrShortName) {
        // Determine the route
        Route dbRoute = getRoute(routeIdOrShortName);
        if (dbRoute == null) return null;

        // Determine the blocks for the route for all service IDs
        List<Block> blocksForRoute = Core.getInstance().getDbConfig().getBlocksForRoute(dbRoute.getId());

        // Convert blocks to list of IpcSchedule objects and return
        return IpcSchedule.createSchedules(dbRoute, blocksForRoute);
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getCurrentCalendars()
     */
    @Override
    public List<IpcCalendar> getCurrentCalendars() {
        // Get list of currently active calendars
        List<Calendar> calendarList = Core.getInstance().getDbConfig().getCurrentCalendars();

        // Convert Calendar list to IpcCalendar list
        List<IpcCalendar> ipcCalendarList = new ArrayList<>();
        for (Calendar calendar : calendarList) {
            ipcCalendarList.add(new IpcCalendar(calendar));
        }

        return ipcCalendarList;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getAllCalendars()
     */
    @Override
    public List<IpcCalendar> getAllCalendars() {
        // Get list of currently active calendars
        List<Calendar> calendarList = Core.getInstance().getDbConfig().getCalendars();

        // Convert Calendar list to IpcCalendar list
        List<IpcCalendar> ipcCalendarList = new ArrayList<>();
        for (Calendar calendar : calendarList) {
            ipcCalendarList.add(new IpcCalendar(calendar));
        }

        return ipcCalendarList;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getVehicleIds()
     */
    @Override
    public List<String> getVehicleIds() {
        Collection<VehicleConfig> vehicleConfigs =
                VehicleDataCache.getInstance().getVehicleConfigs();
        List<String> vehicleIds = new ArrayList<>(vehicleConfigs.size());
        for (VehicleConfig vehicleConfig : vehicleConfigs) {
            vehicleIds.add(vehicleConfig.getId());
        }
        return vehicleIds;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getServiceIds()
     */
    @Override
    public List<String> getServiceIds() {
        // Convert the Set from getServiceIds() to a List since need
        // to use a List for IPC due to serialization.
        return new ArrayList<>(Core.getInstance().getDbConfig().getServiceIds());
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getCurrentServiceIds()
     */
    @Override
    public List<String> getCurrentServiceIds() {
        // Convert the Set from getCurrentServiceIds() to a List since need
        // to use a List for IPC due to serialization.
        return new ArrayList<>(Core.getInstance().getDbConfig().getCurrentServiceIds());
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getTripIds()
     */
    @Override
    public List<String> getTripIds() {
        var trips = Core.getInstance().getDbConfig().getTrips().values();
        return trips.stream()
                .map(Trip::getId)
                .collect(Collectors.toList());
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getBlockIds()
     */
    @Override
    public List<String> getBlockIds() {
        var blocks = Core.getInstance().getDbConfig().getBlocks();
        return blocks.stream()
                .map(Block::getId)
                .distinct()
                .collect(Collectors.toList());
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getBlockIds()
     */
    @Override
    public List<String> getBlockIds(String serviceId) {
        if (serviceId == null) {
            return getBlockIds();
        }

        var blocks = Core.getInstance().getDbConfig().getBlocks(serviceId);
        return blocks.stream()
                .map(Block::getId)
                .distinct()
                .collect(Collectors.toList());
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getBlockIds()
     */
    @Override
    public Map<String, List<String>> getServiceIdsWithBlockIds() {
        return Core.getInstance()
                .getDbConfig()
                .getBlockIdsForAllServiceIds();
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.ConfigInterface#getRoutesByStopId()
     */
    @Override
    public List<IpcRoute> getRoutesByStopId(String stopId) {
        List<IpcRoute> routes = new ArrayList<>();
        if (stopId != null) {
            DbConfig dbConfig = Core.getInstance().getDbConfig();
            if (dbConfig == null) return routes;

            routes = dbConfig.getRoutes().stream()
                    .filter(dbRoute -> dbRoute.getStops().stream()
                            .anyMatch(stop -> stop.getId().equals(stopId)))
                    .map(dbRoute -> new IpcRoute(dbRoute, null, null, null))
                    .collect(Collectors.toList());
        }
        return routes;
    }
}
