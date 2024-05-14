/*
 * This file is part of thetransitclock.org
 *
 * thetransitclock.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * thetransitclock.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with thetransitclock.org .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transitclock.core.avl;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.Device;
import io.swagger.client.model.Position;
import io.swagger.client.model.User;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.transitclock.domain.structs.AvlReport;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static org.transitclock.config.data.TraccarConfig.*;


/**
 * @author Sean Óg Crudden This module integrates TheTransitClock with the API of a traccar
 * server to get vehicle locations.
 * <p>
 * See http://www.traccar.org
 * <p>
 * It uses classes that where generated using the swagger file provided
 * with traccar.
 */
@Slf4j
public class TraccarAVLModule extends PollUrlAvlModule {
    public TraccarAVLModule(String agencyId) {
        super(agencyId);

        useCompression = false;
    }


    private DefaultApi initApiClient() throws RuntimeException {
        DefaultApi api = new DefaultApi();
        ApiClient client = new ApiClient();
        client.setBasePath(TRACCARBASEURL.getValue());
        client.setUsername(TRACCAREMAIL.getValue());
        client.setPassword(TRACCARPASSWORD.getValue());
        api.setApiClient(client);
        return api;
    }


    private User initUser(DefaultApi api) throws RuntimeException {
        User user;
        try {
            user = api
                    .sessionPost(TRACCAREMAIL.getValue(), TRACCARPASSWORD.getValue());
            logger.info("Traccar login succeeded.");
            return user;
        } catch (ApiException e) {
            logger.error(e.getMessage() + e.getCause());
        }
        throw new RuntimeException("Traccar login deny");
    }

    @NonNull
    private final DefaultApi API = initApiClient();
    @NonNull
    private final User USER = initUser(API);

    @Override
    protected void getAndProcessData() throws Exception {

        Collection<AvlReport> avlReportsReadIn = new ArrayList<>();

        List<Device> devices = API.devicesGet(true, USER.getId(), null, null);

        List<Position> results = API.positionsGet(null, null, null, null);
        for (Position result : results) {
            Device device = findDeviceById(devices, result.getDeviceId());

            AvlReport avlReport;

            // If have device details use name.
            if (device != null && device.getUniqueId() != null && !device.getUniqueId().isEmpty()) {
                //Traccar return speed in kt
                avlReport = new AvlReport(device.getUniqueId(), device.getName(),
                        result.getDeviceTime().toDate().getTime(), result.getLatitude().doubleValue(),
                        result.getLongitude().doubleValue(), result.getSpeed().multiply(BigDecimal.valueOf(0.5144444)).floatValue(), result.getCourse().floatValue(), TRACCARSOURCE.toString());
            } else {
                avlReport = new AvlReport(result.getDeviceId().toString(),
                        result.getDeviceTime().toDate().getTime(), result.getLatitude().doubleValue(),
                        result.getLongitude().doubleValue(), result.getSpeed().multiply(BigDecimal.valueOf(0.5144444)).floatValue(), result.getCourse().floatValue(), TRACCARSOURCE.toString());
            }
            if (avlReport != null)
                avlReportsReadIn.add(avlReport);
        }
        forwardAvlReports(avlReportsReadIn);
    }

    protected void forwardAvlReports(Collection<AvlReport> avlReportsReadIn) {
        processAvlReports(avlReportsReadIn);
    }

    private Device findDeviceById(List<Device> devices, Integer id) {
        for (Device device : devices) {
            if (device.getId().equals(id))
                return device;
        }
        return null;
    }

    @Override
    protected Collection<AvlReport> processData(InputStream in) throws Exception {
        // Auto-generated method stub
        return null;
    }

}
