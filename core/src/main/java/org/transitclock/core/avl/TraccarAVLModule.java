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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.transitclock.domain.structs.AvlReport;
import org.transitclock.extension.traccar.ApiClient;
import org.transitclock.extension.traccar.ApiException;
import org.transitclock.extension.traccar.api.DefaultApi;
import org.transitclock.extension.traccar.model.DeviceDto;
import org.transitclock.extension.traccar.model.PositionDto;
import org.transitclock.extension.traccar.model.UserDto;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
    @NonNull
    private final DefaultApi api;
    @NonNull
    private final UserDto user;

    public TraccarAVLModule(String agencyId) throws URISyntaxException {
        super(agencyId);
        useCompression = false;
        ApiClient client;
        try {
            var host = HttpHost.create(TRACCARBASEURL.getValue());
            var httpClientBuilder = HttpClientBuilder.create();

            final AuthCache authCache = new BasicAuthCache();
            authCache.put(host, new BasicScheme());

            var provider = new BasicCredentialsProvider();
            provider.setCredentials(new AuthScope(host), new UsernamePasswordCredentials(TRACCAREMAIL.getValue(), TRACCARPASSWORD.getValue().toCharArray()));
            httpClientBuilder.setDefaultCredentialsProvider(provider);

            client = new ApiClient(httpClientBuilder.build());
        } catch (Exception ex) {
            logger.warn("Unsuccessful traccar authorisation: {} ", ex.toString());
            client = new ApiClient();
        }
        client.setBasePath(TRACCARBASEURL.getValue());
        client.setUsername(TRACCAREMAIL.getValue());
        client.setPassword(TRACCARPASSWORD.getValue());
        this.api = new DefaultApi(client);

        try {
            this.user = this.api
                    .sessionPost(TRACCAREMAIL.getValue(), TRACCARPASSWORD.getValue());
        } catch (ApiException e) {
            throw new RuntimeException("Traccar login denied", e);
        }
    }

    @Override
    protected void getAndProcessData() throws Exception {

        Collection<AvlReport> avlReportsReadIn = new ArrayList<>();

        List<DeviceDto> devices = api.devicesGet(true, user.getId(), null, null);
        List<PositionDto> results = api.positionsGet(null, null, null, null);

        for (PositionDto result : results) {
            DeviceDto device = findDeviceById(devices, result.getDeviceId());

            AvlReport avlReport;

            // If have device details use name.
            if (device != null && device.getUniqueId() != null && !device.getUniqueId().isEmpty()) {
                //Traccar return speed in kt
                avlReport = new AvlReport(device.getUniqueId(), device.getName(),
                        result.getDeviceTime().toEpochSecond() * 1000,
                        result.getLatitude().doubleValue(),
                        result.getLongitude().doubleValue(),
                        result.getSpeed().multiply(BigDecimal.valueOf(0.5144444)).floatValue(),
                        result.getCourse().floatValue(), TRACCARSOURCE.toString());
            } else {
                avlReport = new AvlReport(result.getDeviceId().toString(),
                        result.getDeviceTime().toEpochSecond() * 1000,
                        result.getLatitude().doubleValue(),
                        result.getLongitude().doubleValue(),
                        result.getSpeed().multiply(BigDecimal.valueOf(0.5144444)).floatValue(),
                        result.getCourse().floatValue(), TRACCARSOURCE.toString());
            }
            avlReportsReadIn.add(avlReport);
        }
        forwardAvlReports(avlReportsReadIn);
    }

    protected void forwardAvlReports(Collection<AvlReport> avlReportsReadIn) {
        processAvlReports(avlReportsReadIn);
    }

    private DeviceDto findDeviceById(List<DeviceDto> devices, Integer id) {
        for (DeviceDto device : devices) {
            if (Objects.equals(device.getId(), id)) {
                return device;
            }
        }
        return null;
    }

    @Override
    protected Collection<AvlReport> processData(InputStream in) throws Exception {
        // Auto-generated method stub
        return null;
    }

}
