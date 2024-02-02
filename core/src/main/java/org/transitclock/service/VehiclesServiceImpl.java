/* (C)2023 */
package org.transitclock.service;

import com.querydsl.jpa.impl.JPAQuery;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.transitclock.core.BlocksInfo;
import org.transitclock.core.dataCache.VehicleDataCache;
import org.transitclock.domain.hibernate.HibernateUtils;
import org.transitclock.domain.structs.*;
import org.transitclock.service.contract.VehiclesInterface;
import org.transitclock.service.dto.*;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Implements the VehiclesInterface interface on the server side such that a VehiclessClient can
 * make RMI calls in order to obtain vehicle information. The vehicle information is provided using
 * org.transitclock.ipc.data.Vehicle objects.
 *
 * @author SkiBu Smith
 */
@Service
@Slf4j
public class VehiclesServiceImpl implements VehiclesInterface {
    // The VehicleDataCache associated with the singleton.
    private final VehicleDataCache vehicleDataCache;

    /*
     * Constructor. Made private so that can only be instantiated by
     * get(). Doesn't actually do anything since all the work is done in
     * the superclass constructor.
     *
     * @param projectId
     *            for registering this object with the rmiregistry
     */
    public VehiclesServiceImpl(VehicleDataCache vehicleDataCache) {
        this.vehicleDataCache = vehicleDataCache;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#get()
     */
    @Override
    public Collection<IpcVehicle> get() throws RemoteException {
        return getSerializableCollection(vehicleDataCache.getVehicles());
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getComplete()
     */
    @Override
    public Collection<IpcVehicleComplete> getComplete() throws RemoteException {
        return getCompleteSerializableCollection(vehicleDataCache.getVehicles());
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getGtfsRealtime()
     */
    @Override
    public Collection<IpcVehicleGtfsRealtime> getGtfsRealtime() throws RemoteException {
        return getGtfsRealtimeSerializableCollection(vehicleDataCache.getVehicles());
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#get(java.lang.String)
     */
    @Override
    public IpcVehicle get(String vehicleId) throws RemoteException {
        return vehicleDataCache.getVehicle(vehicleId);
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#get(java.lang.String)
     */
    @Override
    public IpcVehicleComplete getComplete(String vehicleId) throws RemoteException {
        return vehicleDataCache.getVehicle(vehicleId);
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#get(java.util.List)
     */
    @Override
    public Collection<IpcVehicle> get(Collection<String> vehicleIds) throws RemoteException {
        return getSerializableCollection(vehicleDataCache.getVehicles(vehicleIds));
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#get(java.util.List)
     */
    @Override
    public Collection<IpcVehicleComplete> getComplete(Collection<String> vehicleIds) throws RemoteException {
        return getCompleteSerializableCollection(vehicleDataCache.getVehicles(vehicleIds));
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getForRoute(java.lang.String)
     */
    @Override
    public Collection<IpcVehicle> getForRoute(String routeIdOrShortName) throws RemoteException {
        return getSerializableCollection(vehicleDataCache.getVehiclesForRoute(routeIdOrShortName));
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getForRoute(java.lang.String)
     */
    @Override
    public Collection<IpcVehicleComplete> getCompleteForRoute(String routeIdOrShortName) throws RemoteException {
        return getCompleteSerializableCollection(vehicleDataCache.getVehiclesForRoute(routeIdOrShortName));
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getForRoute(java.util.Collection)
     */
    @Override
    public Collection<IpcVehicle> getForRoute(Collection<String> routeIdsOrShortNames) throws RemoteException {
        return getSerializableCollection(vehicleDataCache.getVehiclesForRoute(routeIdsOrShortNames));
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getForRoute(java.util.Collection)
     */
    @Override
    public Collection<IpcVehicleComplete> getCompleteForRoute(Collection<String> routeIdsOrShortNames)
            throws RemoteException {
        return getCompleteSerializableCollection(vehicleDataCache.getVehiclesForRoute(routeIdsOrShortNames));
    }

    /*
     * This class returns Collections of Vehicles that are to be serialized.
     * But sometimes these collections come from Map<K, T>.values(), which
     * is a Collection that is not serializable. For such non-serializable
     * collections this method returns a serializable version.
     */
    private Collection<IpcVehicle> getSerializableCollection(Collection<IpcVehicleComplete> vehicles) {
        // If vehicles is null then return empty array
        if (vehicles == null)
            return new ArrayList<>();

        return new ArrayList<>(vehicles);
    }

    /**
     * This class returns Collection of Vehicles that are to be serialized. But sometimes these
     * collections come from Map<K, T>.values(), which is a Collection that is not serializable. For
     * such non-serializable collections this method returns a serializable version. If vehicles
     * parameter is null then an empty array is returned.
     *
     * @param vehicles Original, possible not serializable, collection of vehicles. Can be null.
     * @return Serializable Collection if IpcGtfsRealtimeVehicle objects.
     */
    private Collection<IpcVehicleGtfsRealtime> getGtfsRealtimeSerializableCollection(
            Collection<IpcVehicleComplete> vehicles) {
        // If vehicles is null then return empty array.
        if (vehicles == null) return new ArrayList<IpcVehicleGtfsRealtime>();

        return new ArrayList<IpcVehicleGtfsRealtime>(vehicles);
    }

    /**
     * This class returns Collection of Vehicles that are to be serialized. But sometimes these
     * collections come from Map<K, T>.values(), which is a Collection that is not serializable. For
     * such non-serializable collections this method returns a serializable version. If vehicles
     * parameter is null then an empty array is returned.
     *
     * @param vehicles Original, possible not serializable, collection of vehicles. Can be null.
     * @return Serializable Collection if IpcCompleteVehicle objects.
     */
    private Collection<IpcVehicleComplete> getCompleteSerializableCollection(Collection<IpcVehicleComplete> vehicles) {
        // If vehicles is null then return empty array.
        if (vehicles == null) return new ArrayList<>();

        if (vehicles instanceof Serializable) {
            return vehicles;
        } else {
            return new ArrayList<IpcVehicleComplete>(vehicles);
        }
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getActiveBlocks()
     */
    @Override
    public Collection<IpcActiveBlock> getActiveBlocks(Collection<String> routeIds, int allowableBeforeTimeSecs)
            throws RemoteException {
        // List of data to be returned
        List<IpcActiveBlock> results = new ArrayList<>();
        // Determine all the active blocks
        List<Block> blocks = BlocksInfo.getCurrentlyActiveBlocks(routeIds, null, allowableBeforeTimeSecs, -1);
        // For each active block determine associated vehicle
        for (Block block : blocks) {
            IpcBlock ipcBlock;
            try {
                ipcBlock = new IpcBlock(block);
            } catch (Exception ex) {
                continue;
            }

            // If a block doesn't have a vehicle associated with it need
            // to determine which route a block is currently associated with
            // since can't get that info from the vehicle. This way the block
            // can be properly grouped with the associated route even when it
            // doesn't have a vehicle assigned.
            int activeTripIndex = block.activeTripIndex(new Date(), allowableBeforeTimeSecs);

            // Determine vehicles associated with the block if there are any
            Collection<String> vehicleIdsForBlock =
                    vehicleDataCache.getVehiclesByBlockId(block.getId());
            Collection<IpcVehicle> ipcVehiclesForBlock = get(vehicleIdsForBlock);

            // Create and add the IpcActiveBlock
            Trip tripForSorting = block.getTrip(activeTripIndex);
            IpcActiveBlock ipcBlockAndVehicle =
                    new IpcActiveBlock(ipcBlock, activeTripIndex, ipcVehiclesForBlock, tripForSorting);
            results.add(ipcBlockAndVehicle);
        }
        // Sort the results so that ordered by route and then block start time
        IpcActiveBlock.sort(results);

        // Return results
        return results;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getActiveBlocks()
     */
    @Override
    public int getNumActiveBlocks(Collection<String> routeIds, int allowableBeforeTimeSecs) throws RemoteException {
        // Determine all the active blocks
        List<Block> blocks = BlocksInfo.getCurrentlyActiveBlocks(routeIds, null, allowableBeforeTimeSecs, -1);

        return blocks.size();
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getActiveBlocks()
     */
    @Override
    public Collection<IpcActiveBlock> getActiveBlocksWithoutVehicles(
            Collection<String> routeIds, int allowableBeforeTimeSecs) throws RemoteException {
        // List of data to be returned
        List<IpcActiveBlock> results = new ArrayList<>();
        // Determine all the active blocks
        List<Block> blocks = BlocksInfo.getCurrentlyActiveBlocks(routeIds, null, allowableBeforeTimeSecs, -1);
        // For each active block determine associated vehicle
        for (Block block : blocks) {
            try {
                IpcBlock ipcBlock = new IpcBlock(block);
                int activeTripIndex = block.activeTripIndex(new Date(), allowableBeforeTimeSecs);

                // Create and add the IpcActiveBlock, skipping the slow vehicle fetching
                Trip tripForSorting = block.getTrip(activeTripIndex);
                IpcActiveBlock ipcBlockAndVehicle =
                        new IpcActiveBlock(ipcBlock, activeTripIndex, new ArrayList<IpcVehicle>(), tripForSorting);
                results.add(ipcBlockAndVehicle);
            } catch (Exception e) {
                logger.warn("Error while fecthing active blocks data (probably hibernate still loading data): {}", e.getMessage());
            }
        }
        // Sort the results so that ordered by route and then block start time
        IpcActiveBlock.sort(results);

        // Return results
        return results;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getActiveBlocksAndVehiclesByRouteId()
     */
    @Override
    public Collection<IpcActiveBlock> getActiveBlocksAndVehiclesByRouteId(String routeId, int allowableBeforeTimeSecs)
            throws RemoteException {

        Collection<String> routeIds = new ArrayList<>();
        routeIds.add(routeId);
        return getActiveBlocksAndVehiclesByRouteId(routeIds, allowableBeforeTimeSecs);
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getActiveBlocksAndVehiclesByRouteName()
     */
    @Override
    public Collection<IpcActiveBlock> getActiveBlocksAndVehiclesByRouteName(
            String routeName, int allowableBeforeTimeSecs) throws RemoteException {

        Session session = HibernateUtils.getSession();
        JPAQuery<Route> query = new JPAQuery<>(session);
        var qentity = QRoute.route;
        List<String> routeIds = query.from(qentity)
                .select(qentity.id)
                .where(qentity.name.eq(routeName))
                .groupBy(qentity.id)
                .fetch();
        session.close();

        return getActiveBlocksAndVehiclesByRouteId(routeIds, allowableBeforeTimeSecs);
    }

    private Collection<IpcActiveBlock> getActiveBlocksAndVehiclesByRouteId(
            Collection<String> routeIds, int allowableBeforeTimeSecs) throws RemoteException {

        // List of data to be returned
        List<IpcActiveBlock> results = new ArrayList<>();
        // Determine all the active blocks
        List<Block> blocks = BlocksInfo.getCurrentlyActiveBlocks(routeIds, null, allowableBeforeTimeSecs, -1);
        // For each active block determine associated vehicle
        for (Block block : blocks) {
            IpcBlock ipcBlock = new IpcBlock(block);
            // If a block doesn't have a vehicle associated with it need
            // to determine which route a block is currently associated with
            // since can't get that info from the vehicle. This way the block
            // can be properly grouped with the associated route even when it
            // doesn't have a vehicle assigned.
            int activeTripIndex = block.activeTripIndex(new Date(), allowableBeforeTimeSecs);

            Trip tripForSorting = block.getTrip(activeTripIndex);

            // Check that block's active trip is for the specified route
            // (Otherwise, could be a past or future trip)
            if (routeIds != null && !routeIds.isEmpty() && !routeIds.contains(tripForSorting.getRouteId())) continue;

            // Determine vehicles associated with the block if there are any
            Collection<String> vehicleIdsForBlock =
                    vehicleDataCache.getVehiclesByBlockId(block.getId());
            Collection<IpcVehicle> ipcVehiclesForBlock = get(vehicleIdsForBlock);

            // Create and add the IpcActiveBlock
            IpcActiveBlock ipcBlockAndVehicle =
                    new IpcActiveBlock(ipcBlock, activeTripIndex, ipcVehiclesForBlock, tripForSorting);
            results.add(ipcBlockAndVehicle);
        }
        // Sort the results so that ordered by route and then block start time
        IpcActiveBlock.sort(results);

        // Return results
        return results;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getVehicleConfigs()
     */
    @Override
    public Collection<IpcVehicleConfig> getVehicleConfigs() throws RemoteException {
        Collection<IpcVehicleConfig> result = new ArrayList<>();
        for (VehicleConfig vehicleConfig : vehicleDataCache.getVehicleConfigs()) {
            result.add(new IpcVehicleConfig(vehicleConfig));
        }

        return result;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.VehiclesInterface#getVehiclesForBlocks()
     */
    @Override
    public Collection<IpcVehicle> getVehiclesForBlocks() throws RemoteException {
        List<String> vehicleIds = new ArrayList<>();
        List<Block> blocks = BlocksInfo.getCurrentlyActiveBlocks();
        for (Block block : blocks) {
            Collection<String> vehicleIdsForBlock =
                    vehicleDataCache.getVehiclesByBlockId(block.getId());
            vehicleIds.addAll(vehicleIdsForBlock);
        }
        return get(vehicleIds);
    }

    @Override
    public Collection<IpcVehicleToBlockConfig> getVehicleToBlockConfig(String blockId) throws RemoteException {
        List<IpcVehicleToBlockConfig> result = new ArrayList<>();
        Session session = HibernateUtils.getSession();
        try {
            for (VehicleToBlockConfig vTBC : VehicleToBlockConfig.getVehicleToBlockConfigsByBlockId(session, blockId)) {
                result.add(new IpcVehicleToBlockConfig(vTBC));
            }
            session.close();
        } catch (Exception ex) {
            session.close();
        }
        return result;
    }

    @Override
    public Collection<IpcVehicleToBlockConfig> getVehicleToBlockConfigByVehicleId(String vehicleId)
            throws RemoteException {
        List<IpcVehicleToBlockConfig> result = new ArrayList<>();
        Session session = HibernateUtils.getSession();
        try {
            for (VehicleToBlockConfig vTBC :
                    VehicleToBlockConfig.getVehicleToBlockConfigsByVehicleId(session, vehicleId)) {
                result.add(new IpcVehicleToBlockConfig(vTBC));
            }
            session.close();
        } catch (Exception ex) {
            session.close();
        }
        return result;
    }
}
