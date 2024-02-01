/* (C)2023 */
package org.transitclock.core;

import lombok.extern.slf4j.Slf4j;
import org.transitclock.Module;
import org.transitclock.SingletonContainer;
import org.transitclock.config.data.AgencyConfig;
import org.transitclock.config.data.TimeoutConfig;
import org.transitclock.core.dataCache.VehicleStateManager;
import org.transitclock.core.schedBasedPreds.SchedBasedPredsModule;
import org.transitclock.domain.structs.AvlReport;
import org.transitclock.domain.structs.VehicleEvent;
import org.transitclock.utils.IntervalTimer;
import org.transitclock.utils.SystemTime;
import org.transitclock.utils.Time;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * For handling when a vehicle doesn't report its position for too long. Makes the vehicle
 * unpredictable if a timeout occurs.
 *
 * <p>Note: only predictable vehicles are timed out. This is because vehicles that are not in
 * service are likely to get turned off and not report their position for a long period of time.
 * Plus since they are already not predictable there is no need to be make them unpredictable when
 * there is a timeout.
 *
 * @author SkiBu Smith
 */
@Slf4j
public class TimeoutHandlerModule extends Module {

    // For keeping track of the last AVL report for each vehicle. Keyed on
    // vehicle ID. Synchronize map modifications since elsewhere the elements
    // can be removed from the map.
    private final Map<String, AvlReport> avlReportsMap = new HashMap<>();

    private final AvlProcessor avlProcessor = SingletonContainer.getInstance(AvlProcessor.class);
    private final VehicleStateManager vehicleStateManager = SingletonContainer.getInstance(VehicleStateManager.class);

    /** Constructor */
    public TimeoutHandlerModule(String agencyId) {
        super(agencyId);
    }

    /**
     * Stores the specified AVL report into map so know the last time received AVL data for the
     * vehicle.
     *
     * @param avlReport AVL report to store
     */
    public void storeAvlReport(AvlReport avlReport) {
        // Synchronize map modifications since elsewhere the elements can be removed
        // from the map.
        synchronized (avlReportsMap) {
            avlReportsMap.put(avlReport.getVehicleId(), avlReport);
        }
    }

    /**
     * Removes the specified vehicle from the VehicleDataCache if configured to do so
     *
     * @param vehicleId Vehicle to remove
     */
    public void removeFromVehicleDataCache(String vehicleId) {
        if (TimeoutConfig.removeTimedOutVehiclesFromVehicleDataCache.getValue()) {
            logger.info("Removing vehicleId={} from VehicleDataCache", vehicleId);
            avlProcessor.removeFromVehicleDataCache(vehicleId);
        }
    }

    /**
     * For regular predictable vehicle that is not a schedule based prediction nor a vehicle at a
     * wait stop. If haven't reported in too long makes the vehicle unpredictable and logs
     * situation.
     */
    private void handlePredictablePossibleTimeout(
            VehicleState vehicleState, long now, Iterator<AvlReport> mapIterator) {
        // If haven't reported in too long...
        long maxNoAvl = TimeoutConfig.allowableNoAvlSecs.getValue() * Time.MS_PER_SEC;
        if (now > vehicleState.getAvlReport().getTime() + maxNoAvl) {
            // Make vehicle unpredictable
            String eventDescription = "Vehicle timed out because it "
                    + "has not reported in "
                    + Time.elapsedTimeStr(now - vehicleState.getAvlReport().getTime())
                    + " while allowable time without an AVL report is "
                    + Time.elapsedTimeStr(maxNoAvl)
                    + " and so was made unpredictable.";
            avlProcessor
                    .makeVehicleUnpredictable(vehicleState.getVehicleId(), eventDescription, VehicleEvent.TIMEOUT);

            // Also log the situation
            logger.info("For vehicleId={} {}", vehicleState.getVehicleId(), eventDescription);

            // Remove vehicle from map for next time looking for timeouts
            mapIterator.remove();

            // Remove vehicle from cache if configured to do so
            removeFromVehicleDataCache(vehicleState.getVehicleId());
        }
    }

    /**
     * For not predictable vehicle. If not removing vehicles from cache, removes the vehicle from
     * the map to avoid looking at it again. If configured to remove timed out vehicles from cache,
     * and haven't reported in too long, removes the vehicle from map and cache.
     *
     * @param mapIterator So can remove AVL report from map
     */
    private void handleNotPredictablePossibleTimeout(VehicleState vehicleState,
                                                     long now,
                                                     Iterator<AvlReport> mapIterator) {
        if (!TimeoutConfig.removeTimedOutVehiclesFromVehicleDataCache.getValue()) {
            // Remove vehicle from map for next time looking for timeouts and return
            mapIterator.remove();
            return;
        }

        // If haven't reported in too long...
        long maxNoAvl = TimeoutConfig.allowableNoAvlSecs.getValue() * Time.MS_PER_SEC;
        if (now > vehicleState.getAvlReport().getTime() + maxNoAvl) {

            // Log the situation
            logger.info("For not predictable vehicleId={} generated timeout " + "event.", vehicleState.getVehicleId());

            // Remove vehicle from map for next time looking for timeouts
            mapIterator.remove();

            // Remove vehicle from cache
            removeFromVehicleDataCache(vehicleState.getVehicleId());
        }
    }

    /**
     * For schedule based predictions. If past the scheduled departure time by more than allowed
     * amount then the schedule based vehicle is removed. Useful for situations such as when using
     * schedule based vehicles and auto assigner but the auto assigner can't find a vehicle for a
     * while, indicating no such vehicle in service.
     */
    private void handleSchedBasedPredsPossibleTimeout(VehicleState vehicleState,
                                                      long now,
                                                      Iterator<AvlReport> mapIterator) {
        // If should timeout the schedule based vehicle...
        String shouldTimeoutEventDescription = SchedBasedPredsModule.shouldTimeoutVehicle(vehicleState, now);
        if (shouldTimeoutEventDescription != null) {
            avlProcessor
                    .makeVehicleUnpredictable(
                            vehicleState.getVehicleId(), shouldTimeoutEventDescription, VehicleEvent.TIMEOUT);

            // Also log the situation
            logger.info(
                    "For schedule based vehicleId={} generated timeout " + "event. {}",
                    vehicleState.getVehicleId(),
                    shouldTimeoutEventDescription);

            // Remove vehicle from map for next time looking for timeouts
            mapIterator.remove();

            // Remove vehicle from cache if configured to do so
            removeFromVehicleDataCache(vehicleState.getVehicleId());
        }
    }

    /**
     * It is a wait stop which means that vehicle can be stopped and turned off for a while such
     * that don't expect to get any AVL reports. Only timeout if past more that the allowed time for
     * wait stops
     */
    private void handleWaitStopPossibleTimeout(VehicleState vehicleState,
                                               long now,
                                               Iterator<AvlReport> mapIterator) {

        // we can't easily determine wait stop time for frequency based trips
        // so don't timeout based on stop info
        if (vehicleState.getBlock().isNoSchedule()) {
            logger.debug("not timing out frequency based assignment {}", vehicleState);
            return;
        }

        // If hasn't been too long between AVL reports then everything is fine
        // and simply return
        long maxNoAvl = TimeoutConfig.allowableNoAvlSecs.getValue() * Time.MS_PER_SEC;
        if (now < vehicleState.getAvlReport().getTime() + maxNoAvl) return;

        // It has been a long time since an AVL report so see if also past the
        // scheduled time for the wait stop
        long scheduledDepartureTime = vehicleState.getMatch().getScheduledWaitStopTime();
        if (scheduledDepartureTime >= 0) {
            // There is a scheduled departure time. Make sure not too
            // far past it
            long maxNoAvlAfterSchedDepartSecs = TimeoutConfig.allowableNoAvlAfterSchedDepartSecs.getValue() * Time.MS_PER_SEC;
            if (now > scheduledDepartureTime + maxNoAvlAfterSchedDepartSecs) {
                // Make vehicle unpredictable
                String stopId = "none (vehicle not matched)";
                if (vehicleState.getMatch() != null) {
                    if (vehicleState.getMatch().getAtEndStop() != null) {
                        stopId = vehicleState.getMatch().getAtStop().getStopId();
                    }
                }
                String eventDescription = "Vehicle timed out because it "
                        + "has not reported AVL location in "
                        + Time.elapsedTimeStr(now - vehicleState.getAvlReport().getTime())
                        + " and it is "
                        + Time.elapsedTimeStr(now - scheduledDepartureTime)
                        + " since the scheduled departure time "
                        + Time.dateTimeStr(scheduledDepartureTime)
                        + " for the wait stop ID "
                        + stopId
                        + " while allowable time without an AVL report is "
                        + Time.elapsedTimeStr(maxNoAvl)
                        + " and maximum allowed time after scheduled departure "
                        + "time without AVL is "
                        + Time.elapsedTimeStr(maxNoAvlAfterSchedDepartSecs)
                        + ". Therefore vehicle was made unpredictable.";
                avlProcessor
                        .makeVehicleUnpredictable(vehicleState.getVehicleId(), eventDescription, VehicleEvent.TIMEOUT);

                // Also log the situation
                logger.info("For vehicleId={} {}", vehicleState.getVehicleId(), eventDescription);

                // Remove vehicle from map for next time looking for timeouts
                mapIterator.remove();

                // Remove vehicle from cache if configured to do so
                removeFromVehicleDataCache(vehicleState.getVehicleId());
            }
        }
    }

    public void handlePossibleTimeouts() {
        // Determine what now is. Don't use System.currentTimeMillis() since
        // that doesn't work for playback.
        long now = SystemTime.getMillis();

        // Sync access to avlReportsMap since it can be simultaneously
        // modified elsewhere
        synchronized (avlReportsMap) {
            // Using an Iterator instead of for(AvlReport a : map.values())
            // because removing elements while iterating. Way to do this without
            // getting concurrent access exception is to use an Iterator.
            Iterator<AvlReport> mapIterator = avlReportsMap.values().iterator();
            while (mapIterator.hasNext()) {
                AvlReport avlReport = mapIterator.next();

                // Get state of vehicle and handle based on it
                VehicleState vehicleState = vehicleStateManager.getVehicleState(avlReport.getVehicleId());

                // Need to synchronize on vehicleState since it might be getting
                // modified via a separate main AVL processing executor thread.
                synchronized (vehicleState) {
                    if (!vehicleState.isPredictable()) {
                        // Vehicle is not predictable
                        handleNotPredictablePossibleTimeout(vehicleState, now, mapIterator);
                    } else if (vehicleState.isForSchedBasedPreds()) {
                        // Handle schedule based predictions vehicle
                        handleSchedBasedPredsPossibleTimeout(vehicleState, now, mapIterator);
                    } else if (vehicleState.isWaitStop()) {
                        // Handle where vehicle is at a wait stop
                        handleWaitStopPossibleTimeout(vehicleState, now, mapIterator);
                    } else {
                        // Not a special case. Simply determine if vehicle
                        // timed out
                        handlePredictablePossibleTimeout(vehicleState, now, mapIterator);
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        logger.info("Starting module {} for agencyId={}", getClass().getName(), getAgencyId());

        // No need to run at startup since haven't processed AVL data yet
        Time.sleep(TimeoutConfig.pollingRateSecs.getValue() * Time.MS_PER_SEC);

        // Run forever
        while (true) {
            try {
                // For determining when to poll next
                IntervalTimer timer = new IntervalTimer();

                // Do the actual work
                handlePossibleTimeouts();

                // Wait appropriate amount of time till poll again
                long sleepTime = TimeoutConfig.pollingRateSecs.getValue() * Time.MS_PER_SEC - timer.elapsedMsec();
                if (sleepTime > 0) {
                    Time.sleep(sleepTime);
                }
            } catch (Exception e) {
                logger.error("Error with TimeoutHandlerModule for agencyId={}", AgencyConfig.getAgencyId(), e);
            }
        }
    }
}
