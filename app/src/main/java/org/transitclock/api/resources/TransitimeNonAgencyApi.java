/* (C)2023 */
package org.transitclock.api.resources;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.web.bind.annotation.RestController;
import org.transitclock.api.data.ApiAgencies;
import org.transitclock.api.data.ApiAgency;
import org.transitclock.api.data.ApiNearbyPredictionsForAgencies;
import org.transitclock.api.data.ApiPredictions;
import org.transitclock.api.predsByLoc.PredsByLoc;
import org.transitclock.api.utils.StandardParameters;
import org.transitclock.api.utils.WebUtils;
import org.transitclock.domain.structs.Agency;
import org.transitclock.domain.structs.Location;
import org.transitclock.domain.webstructs.WebAgency;
import org.transitclock.service.dto.IpcPredictionsForRouteStopDest;
import org.transitclock.service.contract.ConfigInterface;
import org.transitclock.service.contract.PredictionsInterface;
import org.transitclock.service.ConfigServiceImpl;
import org.transitclock.service.PredictionsServiceImpl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains the API commands for the Transitime API for system wide commands, such as determining
 * all agencies. The intent of this feed is to provide what is needed for creating a user interface
 * application, such as a smartphone application.
 *
 * <p>The data output can be in either JSON or XML. The output format is specified by the accept
 * header or by using the query string parameter "format=json" or "format=xml".
 *
 * @author SkiBu Smith
 */
@Path("/key/{key}")
@RestController
public class TransitimeNonAgencyApi {

    private final ConfigInterface configInterface;
    private final PredictionsInterface predictionsInterface;

    public TransitimeNonAgencyApi(ConfigInterface configInterface, PredictionsInterface predictionsInterface) {
        this.configInterface = configInterface;
        this.predictionsInterface = predictionsInterface;
    }

    /**
     * For "agencies" command. Returns information for all configured agencies.
     *
     * @param stdParameters
     * @return
     * @throws WebApplicationException
     */
    @Path("/command/agencies")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Rerives all tha agencies managed by the server.",
            description = "Rerives all tha agencies managed by the server.",
            tags = {"base data", "agency"})
    public Response getAgencies(@BeanParam StandardParameters stdParameters) throws WebApplicationException {
        // Make sure request is valid
        stdParameters.validate();

        // For each agency handled by this server create an ApiAgencies
        // and return the list.
        try {
            List<ApiAgency> apiAgencyList = new ArrayList<ApiAgency>();
            Collection<WebAgency> webAgencies = WebAgency.getCachedOrderedListOfWebAgencies();
            for (WebAgency webAgency : webAgencies) {
                String agencyId = webAgency.getAgencyId();

                // If can't communicate with IPC with that agency then move on
                // to the next one. This is important because some agencies
                // might be declared in the web db but they might not actually
                // be running.
                if (configInterface == null) {
                    // Should really log something here to explain that skipping
                    // agency

                    continue;
                }

                List<Agency> agencies = configInterface.getAgencies();
                for (Agency agency : agencies) {
                    apiAgencyList.add(new ApiAgency(agencyId, agency));
                }
            }
            ApiAgencies apiAgencies = new ApiAgencies(apiAgencyList);
            return stdParameters.createResponse(apiAgencies);
        } catch (RemoteException e) {
            // If problem getting data then return a Bad Request
            throw WebUtils.badRequestException(e);
        }
    }

    /**
     * For "predictionsByLoc" command when want to return data for any agency instead of a single
     * specific one.
     *
     * @param stdParameters StdParametersBean that gets the standard parameters from the URI, query
     *     string, and headers.
     * @param lat latitude in decimal degrees
     * @param lon longitude in decimal degrees
     * @param maxDistance How far away a stop can be from the lat/lon. Default is 1,500 m.
     * @param numberPredictions Maximum number of predictions to return. Default value is 3.
     * @return
     * @throws WebApplicationException
     */
    @Path("/command/predictionsByLoc")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Gets predictions from server by location",
            description = "Gets a list of prediction by location for all angencies managed by the api.",
            tags = {"prediction"})
    public Response getPredictions(
            @BeanParam StandardParameters stdParameters,
            @QueryParam(value = "lat") Double lat,
            @QueryParam(value = "lon") Double lon,
            @QueryParam(value = "maxDistance") @DefaultValue("1500.0") double maxDistance,
            @QueryParam(value = "numPreds") @DefaultValue("3") int numberPredictions)
            throws WebApplicationException {
        // Make sure request is valid
        stdParameters.validate();

        if (maxDistance > PredsByLoc.MAX_MAX_DISTANCE)
            throw WebUtils.badRequestException("Maximum maxDistance parameter "
                    + "is "
                    + PredsByLoc.MAX_MAX_DISTANCE
                    + "m but "
                    + maxDistance
                    + "m was specified in the request.");

        try {
            ApiNearbyPredictionsForAgencies predsForAgencies = new ApiNearbyPredictionsForAgencies();

            // For each nearby agency...
            List<String> nearbyAgencies = PredsByLoc.getNearbyAgencies(lat, lon, maxDistance);
            for (String agencyId : nearbyAgencies) {
                // Get predictions by location for the agency
                List<IpcPredictionsForRouteStopDest> predictions =
                        predictionsInterface.get(new Location(lat, lon), maxDistance, numberPredictions);

                // Convert predictions to API object
                ApiPredictions predictionsData = new ApiPredictions(predictions);

                // Add additional agency related info so can describe the
                // agency in the API.
                WebAgency webAgency = WebAgency.getCachedWebAgency(agencyId);
                String agencyName = webAgency.getAgencyName();
                predictionsData.set(agencyId, agencyName);

                // Add the predictions for the agency to the predictions to
                // be returned
                predsForAgencies.addPredictionsForAgency(predictionsData);
            }
            return stdParameters.createResponse(predsForAgencies);
        } catch (Exception e) {
            // If problem getting data then return a Bad Request
            throw WebUtils.badRequestException(e);
        }
    }
}
