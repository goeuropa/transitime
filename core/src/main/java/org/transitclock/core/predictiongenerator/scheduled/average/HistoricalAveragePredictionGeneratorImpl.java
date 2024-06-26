/* (C)2023 */
package org.transitclock.core.predictiongenerator.scheduled.average;

import lombok.extern.slf4j.Slf4j;
import org.transitclock.Core;
import org.transitclock.config.data.CoreConfig;
import org.transitclock.config.data.PredictionConfig;
import org.transitclock.core.Indices;
import org.transitclock.core.VehicleState;
import org.transitclock.core.dataCache.HistoricalAverage;
import org.transitclock.core.dataCache.StopPathCacheKey;
import org.transitclock.core.dataCache.StopPathPredictionCache;
import org.transitclock.core.dataCache.scheduled.ScheduleBasedHistoricalAverageCache;
import org.transitclock.core.predictiongenerator.PredictionComponentElementsGenerator;
import org.transitclock.core.predictiongenerator.lastvehicle.LastVehiclePredictionGeneratorImpl;
import org.transitclock.domain.structs.AvlReport;
import org.transitclock.domain.structs.PredictionForStopPath;
import org.transitclock.utils.SystemTime;

/**
 * @author Sean Óg Crudden This provides a prediction based on the average of historical data for
 *     schedules based services. The average is taken from the HistoricalAverageCache which is
 *     populated each time an arrival/departure event occurs. The HistoricalAverageCache is updated
 *     using data from the TripDataHistory cache.
 */
@Slf4j
public class HistoricalAveragePredictionGeneratorImpl extends LastVehiclePredictionGeneratorImpl
        implements PredictionComponentElementsGenerator {
    private final String alternative = "LastVehiclePredictionGeneratorImpl";

    /* (non-Javadoc)
     * @see org.transitclock.core.predictiongenerator.KalmanPredictionGeneratorImpl#getTravelTimeForPath(org.transitclock.core.Indices, org.transitclock.db.structs.AvlReport)
     */
    @Override
    public long getTravelTimeForPath(Indices indices, AvlReport avlReport, VehicleState vehicleState) {

        logger.debug("Calling historical average algorithm : {}", indices.toString());
        /*
         * if we have enough data start using historical average otherwise
         * revert to default. This does not mean that this method of
         * prediction is better than the default.
         */
        StopPathCacheKey historicalAverageCacheKey =
                new StopPathCacheKey(indices.getTrip().getId(), indices.getStopPathIndex());

        HistoricalAverage average =
                ScheduleBasedHistoricalAverageCache.getInstance().getAverage(historicalAverageCacheKey);

        if (average != null && average.getCount() >= PredictionConfig.minDays.getValue()) {
            if (CoreConfig.storeTravelTimeStopPathPredictions.getValue()) {
                PredictionForStopPath predictionForStopPath = new PredictionForStopPath(
                        vehicleState.getVehicleId(),
                        SystemTime.getDate(),
                        average.getAverage(),
                        indices.getTrip().getId(),
                        indices.getStopPathIndex(),
                        "HISTORICAL AVERAGE",
                        true,
                        null);
                Core.getInstance().getDbLogger().add(predictionForStopPath);
                StopPathPredictionCache.getInstance().putPrediction(predictionForStopPath);
            }

            logger.debug("Using historical average algorithm for prediction : {} instead of {} prediction: {} for : {}", average, alternative, super.getTravelTimeForPath(indices, avlReport, vehicleState), indices);

            return (long) average.getAverage();
        }

        // logger.debug("No historical average found, generating prediction using lastvehicle
        // algorithm: " + historicalAverageCacheKey.toString());
        /* default to parent method if not enough data. This will be based on schedule if UpdateTravelTimes has not been called. */
        return super.getTravelTimeForPath(indices, avlReport, vehicleState);
    }

    @Override
    public long getStopTimeForPath(Indices indices, AvlReport avlReport, VehicleState vehicleState) {

        StopPathCacheKey historicalAverageCacheKey =
                new StopPathCacheKey(indices.getTrip().getId(), indices.getStopPathIndex(), false);

        HistoricalAverage average =
                ScheduleBasedHistoricalAverageCache.getInstance().getAverage(historicalAverageCacheKey);

        if (average != null && average.getCount() >= PredictionConfig.minDays.getValue()) {
            logger.debug("Using historical average alogrithm for dwell time prediction : {} instead of {} prediction: {} for : {}", average, alternative, super.getStopTimeForPath(indices, avlReport, vehicleState), indices);
            return (long) average.getAverage();
        }

        return super.getStopTimeForPath(indices, avlReport, vehicleState);
    }
}
