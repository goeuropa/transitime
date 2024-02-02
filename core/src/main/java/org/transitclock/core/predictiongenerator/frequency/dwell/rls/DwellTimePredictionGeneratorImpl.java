/* (C)2023 */
package org.transitclock.core.predictiongenerator.frequency.dwell.rls;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.transitclock.config.data.CoreConfig;
import org.transitclock.core.Indices;
import org.transitclock.core.TravelTimes;
import org.transitclock.core.VehicleState;
import org.transitclock.core.dataCache.*;
import org.transitclock.core.dataCache.frequency.FrequencyBasedHistoricalAverageCache;
import org.transitclock.core.holdingmethod.HoldingTimeGenerator;
import org.transitclock.core.predictiongenerator.bias.BiasAdjuster;
import org.transitclock.core.predictiongenerator.datafilter.TravelTimeDataFilter;
import org.transitclock.core.predictiongenerator.frequency.traveltime.kalman.KalmanPredictionGeneratorImpl;
import org.transitclock.domain.hibernate.DataDbLogger;
import org.transitclock.domain.structs.AvlReport;
import org.transitclock.domain.structs.Headway;
import org.transitclock.gtfs.DbConfig;

/**
 * @author Sean Og Crudden
 *     <p>This is an experiment to see if headway can be used to better predict dwell time. Most of
 *     what I have read tells me it can but in conjunction with APC data and estimation of demand at
 *     stops.
 *     <p>This is for frequency based services.
 */
@Slf4j
public class DwellTimePredictionGeneratorImpl extends KalmanPredictionGeneratorImpl {
    private final DwellTimeModelCacheInterface dwellTimeModelCacheInterface;

    public DwellTimePredictionGeneratorImpl(DbConfig dbConfig,
                                            StopArrivalDepartureCacheInterface stopArrivalDepartureCacheInterface,
                                            TripDataHistoryCacheInterface tripDataHistoryCacheInterface,
                                            VehicleStateManager vehicleStateManager, HoldingTimeCache holdingTimeCache, StopPathPredictionCache stopPathPredictionCache, TravelTimes travelTimes, DataDbLogger dataDbLogger, VehicleDataCache vehicleDataCache, FrequencyBasedHistoricalAverageCache frequencyBasedHistoricalAverageCache, DwellTimeModelCacheInterface dwellTimeModelCacheInterface,
                                            ErrorCache errorCache,
                                            HoldingTimeGenerator holdingTimeGenerator,
                                            BiasAdjuster biasAdjuster,
                                            TravelTimeDataFilter travelTimeDataFilter) {
        super(dbConfig, stopArrivalDepartureCacheInterface, tripDataHistoryCacheInterface, vehicleStateManager, holdingTimeCache, stopPathPredictionCache, travelTimes, dataDbLogger, vehicleDataCache, frequencyBasedHistoricalAverageCache, errorCache, holdingTimeGenerator, biasAdjuster, travelTimeDataFilter);
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
                    // TODO Would be more correct to use the start time of the trip.
                    int time = FrequencyBasedHistoricalAverageCache.secondsFromMidnight(new Date(avlReport.getTime()), 2);

                    time = FrequencyBasedHistoricalAverageCache.round(time, CoreConfig.getCacheIncrementsForFrequencyService());

                    StopPathCacheKey cacheKey = new StopPathCacheKey(
                            indices.getTrip().getId(), indices.getStopPathIndex(), false, (long) time);

                    if (dwellTimeModelCacheInterface != null)
                        result = dwellTimeModelCacheInterface.predictDwellTime(cacheKey, headway);

                    if (result == null) {
                        logger.debug(
                                "Using scheduled value for dwell time as no RLS data available for" + " {}.", indices);
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
