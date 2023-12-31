/* (C)2023 */
package org.transitclock.api.rootResources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.transitclock.api.data.siri.SiriStopMonitoring;
import org.transitclock.api.data.siri.SiriVehiclesMonitoring;
import org.transitclock.api.utils.StandardParameters;
import org.transitclock.api.utils.WebUtils;
import org.transitclock.ipc.data.IpcPrediction;
import org.transitclock.ipc.data.IpcPredictionsForRouteStopDest;
import org.transitclock.ipc.data.IpcVehicleComplete;
import org.transitclock.ipc.interfaces.PredictionsInterface;
import org.transitclock.ipc.interfaces.VehiclesInterface;

/**
 * The Siri API
 *
 * @author SkiBu Smith
 */
@Path("/key/{key}/agency/{agency}")
public class SiriApi {

    /********************** Member Functions **************************/

    /**
     * Returns vehicleMonitoring vehicle information in SIRI format. Can specify vehicleIds,
     * routeIds, or routeShortNames to get subset of data. If not specified then vehicle information
     * for entire agency is returned.
     *
     * @param stdParameters
     * @param vehicleIds List of vehicle IDs
     * @param routesIdOrShortNames List of routes
     * @return The response
     * @throws WebApplicationException
     */
    @Path("/command/siri/vehicleMonitoring")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Returns vehicleMonitoring vehicle information in SIRI format.",
            description = "It is possible to specify vehicleIds, routeIds, or routeShortNames to get"
                    + " subset of data. If not specified then vehicle information for entire"
                    + " agency is returned.",
            tags = {"SIRI", "feed"})
    public Response getVehicles(
            @BeanParam StandardParameters stdParameters,
            @Parameter(description = "List of vehicles id", required = false) @QueryParam(value = "v")
                    List<String> vehicleIds,
            @Parameter(description = "List of routesId or routeShortName", required = false) @QueryParam(value = "r")
                    List<String> routesIdOrShortNames)
            throws WebApplicationException {
        // Make sure request is valid
        stdParameters.validate();

        try {
            // Get Vehicle data from server
            VehiclesInterface inter = stdParameters.getVehiclesInterface();

            Collection<IpcVehicleComplete> vehicles;
            if (!routesIdOrShortNames.isEmpty()) {
                vehicles = inter.getCompleteForRoute(routesIdOrShortNames);
            } else if (!vehicleIds.isEmpty()) {
                vehicles = inter.getComplete(vehicleIds);
            } else {
                vehicles = inter.getComplete();
            }

            // Determine and return SiriStopMonitoring response
            SiriVehiclesMonitoring siriVehicles = new SiriVehiclesMonitoring(vehicles, stdParameters.getAgencyId());
            return stdParameters.createResponse(siriVehicles);
        } catch (Exception e) {
            // If problem getting data then return a Bad Request
            throw WebUtils.badRequestException(e);
        }
    }

    /**
     * Returns stopMonitoring vehicle information in SIRI format. Can specify routeId or
     * routeShortName. Need to also specify stopId. Can optionally specify how many max number of
     * predictions per stop to return.
     *
     * @param stdParameters
     * @param routeIdOrShortName
     * @param stopId
     * @param numberPredictions
     * @return
     * @throws WebApplicationException
     */
    @Path("/command/siri/stopMonitoring")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Returns stopMonitoring vehicle information in SIRI format.",
            description = "It is possible to specify vehicleIds, routeIds, or routeShortNames to get"
                    + " subset of data. It is possible to specify the number of perdictions per"
                    + " stop. If not specified then vehicle information for entire agency is"
                    + " returned.",
            tags = {"SIRI", "feed"})
    public Response getVehicles(
            @BeanParam StandardParameters stdParameters,
            @Parameter(description = "RoutesId or routeShortName", required = true) @QueryParam(value = "r")
                    String routeIdOrShortName,
            @Parameter(description = "StopIds", required = true) @QueryParam(value = "s") String stopId,
            @Parameter(description = "Number of predictions", required = false)
                    @QueryParam(value = "numPreds")
                    @DefaultValue("3")
                    int numberPredictions)
            throws WebApplicationException {
        // Make sure request is valid
        stdParameters.validate();

        try {
            // Get prediction data from server
            PredictionsInterface inter = stdParameters.getPredictionsInterface();

            List<IpcPredictionsForRouteStopDest> preds = inter.get(routeIdOrShortName, stopId, numberPredictions);

            // For each prediction also need corresponding vehicle so can create
            // the absurdly large MonitoredVehicleJourney element.
            List<String> vehicleIds = new ArrayList<String>();
            for (IpcPredictionsForRouteStopDest predsForDest : preds) {
                for (IpcPrediction individualPred : predsForDest.getPredictionsForRouteStop()) {
                    vehicleIds.add(individualPred.getVehicleId());
                }
            }
            VehiclesInterface vehicleInter = stdParameters.getVehiclesInterface();
            Collection<IpcVehicleComplete> vehicles = vehicleInter.getComplete(vehicleIds);

            // Determine SiriStopMonitoring response
            SiriStopMonitoring siriStopMonitoring =
                    new SiriStopMonitoring(preds, vehicles, stdParameters.getAgencyId());

            // Return SiriStopMonitoring response
            return stdParameters.createResponse(siriStopMonitoring);
        } catch (Exception e) {
            // If problem getting data then return a Bad Request
            throw WebUtils.badRequestException(e);
        }
    }
}