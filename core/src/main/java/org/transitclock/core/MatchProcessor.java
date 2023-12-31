/* (C)2023 */
package org.transitclock.core;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.transitclock.applications.Core;
import org.transitclock.configData.CoreConfig;
import org.transitclock.core.dataCache.PredictionDataCache;
import org.transitclock.db.structs.Headway;
import org.transitclock.db.structs.Match;
import org.transitclock.db.structs.Prediction;
import org.transitclock.ipc.data.IpcPrediction;
import org.transitclock.utils.Time;

/**
 * For generating predictions, arrival/departure times, headways etc. This class is used once a
 * vehicle is successfully matched to an assignment.
 *
 * @author SkiBu Smith
 */
@Slf4j
public class MatchProcessor {

    // Singleton class
    private static final MatchProcessor singleton = new MatchProcessor();

    /** Constructor declared private because singleton class */
    private MatchProcessor() {}

    /**
     * Returns singleton MatchProcessor
     *
     * @return
     */
    public static MatchProcessor getInstance() {
        return singleton;
    }

    /**
     * Generates the new predictions for the vehicle based on the new match stored in the vehicle
     * state. Updates vehicle state, the predictions cache, and stores predictions in database.
     *
     * @param vehicleState
     */
    private void processPredictions(VehicleState vehicleState) {
        logger.debug("Processing predictions for vehicleId={}", vehicleState.getVehicleId());

        // Generate the new predictions for the vehicle
        List<IpcPrediction> newPredictions =
                PredictionGeneratorFactory.getInstance().generate(vehicleState);

        // Store the predictions in database if so configured
        if (CoreConfig.getMaxPredictionsTimeForDbSecs() > 0) {
            for (IpcPrediction prediction : newPredictions) {
                // If prediction not too far into the future then ...
                if (prediction.getPredictionTime() - prediction.getAvlTime()
                        < ((long) CoreConfig.getMaxPredictionsTimeForDbSecs() * Time.MS_PER_SEC)) {
                    // Store the prediction into db
                    Prediction dbPrediction = new Prediction(prediction);

                    Core.getInstance().getDbLogger().add(dbPrediction);

                } else {
                    logger.debug(
                            "Difference in predictionTiem and AVLTime is {} and is greater than"
                                    + " getMaxPredictionsTimeForDbSecs {}.",
                            prediction.getPredictionTime() - prediction.getAvlTime(),
                            CoreConfig.getMaxPredictionsTimeForDbSecs() * Time.MS_PER_SEC);
                }
            }
        }

        // Update the predictions cache to use the new predictions for the
        // vehicle
        List<IpcPrediction> oldPredictions = vehicleState.getPredictions();
        PredictionDataCache.getInstance().updatePredictions(oldPredictions, newPredictions);

        // Update predictions for vehicle
        vehicleState.setPredictions(newPredictions);
    }

    /**
     * Generates the headway info based on the new match stored in the vehicle state.
     *
     * @param vehicleState
     */
    private void processHeadways(VehicleState vehicleState) {
        logger.debug("Processing headways for vehicleId={}", vehicleState.getVehicleId());

        Headway headway = HeadwayGeneratorFactory.getInstance().generate(vehicleState);

        if (headway != null) {
            vehicleState.setHeadway(headway);
            Core.getInstance().getDbLogger().add(headway);
        }
    }

    /**
     * Generates the arrival/departure info based on the new match stored in the vehicle state.
     * Stores the arrival/departure info into database.
     *
     * @param vehicleState
     */
    private void processArrivalDepartures(VehicleState vehicleState) {
        logger.debug("Processing arrivals/departures for vehicleId={}", vehicleState.getVehicleId());

        ArrivalDepartureGeneratorFactory.getInstance().generate(vehicleState);
    }

    /**
     * Stores the spatial match in log file and to database so can be processed later to determine
     * expected travel times.
     *
     * @param vehicleState
     */
    private void processSpatialMatch(VehicleState vehicleState) {
        logger.debug("Processing spatial match for vehicleId={}", vehicleState.getVehicleId());

        Match match = new Match(vehicleState);

        // Store match in database if it is not at a stop. The reason only
        // storing to db if not at a stop is because reason for storing
        // matches is for determining travel times. But when determining
        // travel times using departure and arrival times at the stops.
        // The matches are only used for in between the stops. And in fact,
        // matches at stops only confuse things since they will be before
        // the departure time or after the arrival time. Plus not storing
        // the matches at the stops means there is less data to store.
        if (!match.isAtStop()) Core.getInstance().getDbLogger().add(match);
    }

    /**
     * Called when vehicle is matched successfully. Generates predictions arrival/departure times,
     * headways and such. But if vehicle should be ignored because part of consist then don't do
     * anything.
     *
     * @param vehicleState
     */
    public void generateResultsOfMatch(VehicleState vehicleState) {
        // Make sure everything ok
        if (!vehicleState.isPredictable()) {
            logger.error(
                    "Vehicle was unpredictable when " + "MatchProcessor.generateResultsOfMatch() was called. {}",
                    vehicleState);
            return;
        }

        // If non-lead vehicle in a consist then don't need to do anything here
        // because all the info will be generated by the lead vehicle.
        if (vehicleState.getAvlReport().ignoreBecauseInConsist()) {
            logger.debug(
                    "Not generating results such as predictions for "
                            + "vehicleId={} because it is a non-lead vehicle in "
                            + "a consist.",
                    vehicleState.getVehicleId());
            return;
        }

        logger.debug("Processing results for match for {}", vehicleState);

        // Process predictions, headways, arrivals/departures, and and spatial
        // matches. If don't need matches then don't store them
        if (!CoreConfig.onlyNeedArrivalDepartures()) {
            processPredictions(vehicleState);
            processHeadways(vehicleState);
            processSpatialMatch(vehicleState);
        }
        processArrivalDepartures(vehicleState);
    }
}