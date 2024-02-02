/* (C)2023 */
package org.transitclock.core.predictiongenerator.scheduled.dwell;

import lombok.extern.slf4j.Slf4j;
import org.transitclock.core.Indices;
import org.transitclock.core.TravelTimes;
import org.transitclock.core.VehicleState;
import org.transitclock.core.dataCache.*;
import org.transitclock.core.holdingmethod.HoldingTimeGenerator;
import org.transitclock.core.predictiongenerator.bias.BiasAdjuster;
import org.transitclock.core.predictiongenerator.datafilter.TravelTimeDataFilter;
import org.transitclock.core.predictiongenerator.scheduled.traveltime.kalman.KalmanPredictionGeneratorImpl;
import org.transitclock.domain.hibernate.DataDbLogger;
import org.transitclock.domain.structs.AvlReport;
import org.transitclock.domain.structs.Headway;
import org.transitclock.gtfs.DbConfig;

/**
 * @author Sean Og Crudden
 *     <p>This is an experiment to see if headway can be used to better predict dwell time. Most of
 *     what I have read tells me it can but in conjunction with APC data and estimation of demand at
 *     stops.
 *     <p>I do wonder if headway alone is enough to at least improve things beyond using the
 *     schedule?
 *     <p>This has now been changed to work with any DwellModel implementation.
 */
@Slf4j
public class DwellTimePredictionGeneratorImpl extends KalmanPredictionGeneratorImpl {
    protected final DwellTimeModelCacheInterface dwellTimeModelCacheInterface;

    public DwellTimePredictionGeneratorImpl(DbConfig dbConfig,
                                            StopArrivalDepartureCacheInterface stopArrivalDepartureCacheInterface,
                                            TripDataHistoryCacheInterface tripDataHistoryCacheInterface,
                                            HoldingTimeCache holdingTimeCache,
                                            ErrorCache kalmanErrorCache,
                                            StopPathPredictionCache stopPathPredictionCache,
                                            VehicleStateManager vehicleStateManager,
                                            TravelTimes travelTimes,
                                            DataDbLogger dataDbLogger,
                                            DwellTimeModelCacheInterface dwellTimeModelCacheInterface,
                                            HoldingTimeGenerator holdingTimeGenerator,
                                            BiasAdjuster biasAdjuster,
                                            TravelTimeDataFilter travelTimeDataFilter) {
        super(dbConfig,stopArrivalDepartureCacheInterface, tripDataHistoryCacheInterface,  holdingTimeCache, kalmanErrorCache, stopPathPredictionCache, vehicleStateManager, travelTimes, dataDbLogger, holdingTimeGenerator, biasAdjuster, travelTimeDataFilter);
        this.dwellTimeModelCacheInterface = dwellTimeModelCacheInterface;
    }

    @Override
    public long getStopTimeForPath(Indices indices, AvlReport avlReport, VehicleState vehicleState) {
        Long result = null;
        try {
            Headway headway = vehicleState.getHeadway();

            if (headway != null) {
                logger.debug("Headway at {} based on avl {} is {}.", indices, avlReport, headway);

                /* Change approach to use a RLS model.
                 */
                if (super.getStopTimeForPath(indices, avlReport, vehicleState) > 0) {

                    StopPathCacheKey cacheKey =
                            new StopPathCacheKey(indices.getTrip().getId(), indices.getStopPathIndex(), false);

                    if (dwellTimeModelCacheInterface != null)
                        result = dwellTimeModelCacheInterface.predictDwellTime(cacheKey, headway);

                    if (result == null) {
                        logger.debug(
                                "Using scheduled value for dwell time as no model available for" + " {}.", indices);
                        result = super.getStopTimeForPath(indices, avlReport, vehicleState);
                    }

                    /* should never have a negative dwell time */
                    if (result < 0) {
                        logger.debug("Predicted negative dwell time {} for {}.", result, indices);
                        result = 0L;
                    }

                } else {
                    logger.debug("Scheduled dwell time is less than 0 for {}.", indices);
                    result = super.getStopTimeForPath(indices, avlReport, vehicleState);
                }

                logger.debug(
                        "Using dwell time {} for {} instead of {}. Headway for vehicle {} is {}",
                        result,
                        indices,
                        super.getStopTimeForPath(indices, avlReport, vehicleState),
                        vehicleState.getVehicleId(),
                        headway);
            } else {
                result = super.getStopTimeForPath(indices, avlReport, vehicleState);
                logger.debug(
                        "Using dwell time {} for {} instead of {}. No headway.",
                        result,
                        indices,
                        super.getStopTimeForPath(indices, avlReport, vehicleState));
            }

        } catch (Exception e) {

            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }

        return result;
    }
}
