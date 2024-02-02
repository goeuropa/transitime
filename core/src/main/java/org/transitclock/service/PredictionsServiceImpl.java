/* (C)2023 */
package org.transitclock.service;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.transitclock.core.dataCache.PredictionDataCache;
import org.transitclock.domain.structs.Location;
import org.transitclock.gtfs.StopsByLocation;
import org.transitclock.gtfs.StopsByLocation.StopInfo;
import org.transitclock.service.dto.IpcPredictionsForRouteStopDest;
import org.transitclock.service.contract.PredictionsInterface;
import org.transitclock.utils.IntervalTimer;
import org.transitclock.utils.SystemTime;
import org.transitclock.utils.Time;

/**
 * Implements the PredictionsInterface interface on the server side such that a
 * PredictionsInterfaceFactory can make RMI calls in order to obtain prediction information. The
 * prediction information is provided using org.transitclock.ipc.data.Prediction objects.
 *
 * @author SkiBu Smith
 */
@Service
@Slf4j
public class PredictionsServiceImpl implements PredictionsInterface {
    // The PredictionDataCache associated with the singleton.
    private final PredictionDataCache predictionDataCache;


    /*
     * Constructor. Made private so that can only be instantiated by
     * get(). Doesn't actually do anything since all the work is done in
     * the superclass constructor.
     *
     * @param projectId
     *            for registering this object with the rmiregistry
     */
    public PredictionsServiceImpl(PredictionDataCache predictionDataCache) {
        this.predictionDataCache = predictionDataCache;
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.PredictionsInterface#get(java.lang.String, java.lang.String, int)
     */
    @Override
    public List<IpcPredictionsForRouteStopDest> get(String routeIdOrShortName, String stopId, int predictionsPerStop)
            throws RemoteException {
        return predictionDataCache.getPredictions(routeIdOrShortName, null, stopId, predictionsPerStop);
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.PredictionsInterface#get(java.util.List, int)
     */
    @Override
    public List<IpcPredictionsForRouteStopDest> get(List<RouteStop> routeStops, int predictionsPerStop)
            throws RemoteException {
        return predictionDataCache.getPredictions(routeStops, predictionsPerStop);
    }

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.PredictionsInterface#getPredictionsByVehicle()
     */
    @Override
    public List<IpcPredictionsForRouteStopDest> getAllPredictions(int predictionMaxFutureSecs) {
        // How far in future in absolute time should get predictions for
        long maxSystemTimeForPrediction =
                SystemTime.getMillis() + (long) predictionMaxFutureSecs * Time.MS_PER_SEC;

        return predictionDataCache.getAllPredictions(Integer.MAX_VALUE, maxSystemTimeForPrediction);
    }

    // If stops are relatively close then should order routes based on route
    // order instead of distance.
    private static final double DISTANCE_AT_WHICH_ROUTES_GROUPED = 80.0;

    /**
     * For sorting resulting predictions so that they are by how close the stop is away, and then by
     * route order, and then by direction. This way the most likely useful stops are displayed
     * first.
     */
    private static final Comparator<IpcPredictionsForRouteStopDest> predsByLocComparator =
            new Comparator<IpcPredictionsForRouteStopDest>() {

                public int compare(IpcPredictionsForRouteStopDest pred1, IpcPredictionsForRouteStopDest pred2) {
                    // If route order indicates pred1 before pred2...
                    int routeOrderDifference = pred1.getRouteOrder() - pred2.getRouteOrder();
                    if (routeOrderDifference < 0) {
                        // route order indicates pred1 before pred2.
                        // Pred1 should be before pred2 unless much closer to pred2
                        if (pred1.getDistanceToStop() > pred2.getDistanceToStop() + DISTANCE_AT_WHICH_ROUTES_GROUPED)
                            return 1; // pred2 is much closer
                        else return -1; // pred2 not much closer so use route order
                    } else if (routeOrderDifference == 0) {
                        // Route order indicates that pred1 order same as pred2
                        return pred1.getDirectionId().compareTo(pred2.getDirectionId());
                    } else {
                        // Route order indicates pred1 after pred2.
                        // Pred2 should be before pred1 unless much closer to pred1
                        if (pred2.getDistanceToStop() > pred1.getDistanceToStop() + DISTANCE_AT_WHICH_ROUTES_GROUPED)
                            return -1; // pred1 is much closer
                        else return 1; // pred1 not much closer so use route order
                    }
                }
            };

    /* (non-Javadoc)
     * @see org.transitclock.ipc.interfaces.PredictionsInterface#get(org.transitclock.db.structs.Location, double, int)
     */
    @Override
    public List<IpcPredictionsForRouteStopDest> get(Location loc, double maxDistance, int predictionsPerStop)
            throws RemoteException {
        IntervalTimer timer = new IntervalTimer();

        // For returning the results
        List<IpcPredictionsForRouteStopDest> results = new ArrayList<IpcPredictionsForRouteStopDest>();

        // Determine which stops are near the location
        List<StopInfo> stopInfos = StopsByLocation.getStops(loc, maxDistance);

        // Gather predictions for all of those stops
        for (StopInfo stopInfo : stopInfos) {
            // Get the predictions for the stop
            List<IpcPredictionsForRouteStopDest> predictionsForStop = predictionDataCache.getPredictions(
                    stopInfo.routeShortName,
                    stopInfo.tripPattern.getDirectionId(),
                    stopInfo.stopId,
                    predictionsPerStop,
                    stopInfo.distanceToStop);

            // Add info from this stop to the results
            if (predictionsForStop.isEmpty()) {
                // No predictions for this stop but should still add it to
                // results in case the user interface wants to show nearby stops
                // for routes that are not currently in service. This could be
                // useful to show messages, such as there being no service for
                // the route due to a parade.
                IpcPredictionsForRouteStopDest emptyPredsForStop = new IpcPredictionsForRouteStopDest(
                        stopInfo.tripPattern, stopInfo.stopId, stopInfo.distanceToStop);
                results.add(emptyPredsForStop);
            } else {
                // There are predictions for this stop so add them to the results
                results.addAll(predictionsForStop);
            }
        }

        // Sort the predictions so that nearby stops output first, stops of
        // similar distance are output in route order, and direction "0"
        // is output first for each route.
        Collections.sort(results, predsByLocComparator);

        logger.info("Determined predictions for stops near {}. Took {} msec", loc, timer.elapsedMsec());

        // Return all of the predictions
        return results;
    }
}
