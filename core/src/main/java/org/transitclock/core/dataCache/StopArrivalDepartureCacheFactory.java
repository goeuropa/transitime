/* (C)2023 */
package org.transitclock.core.dataCache;

import org.transitclock.annotations.Bean;
import org.transitclock.annotations.Configuration;
import org.transitclock.config.StringConfigValue;
import org.transitclock.utils.ClassInstantiator;

/**
 * @author Sean Óg Crudden
 */
@Configuration
public class StopArrivalDepartureCacheFactory {
    private static final StringConfigValue className = new StringConfigValue(
            "transitclock.core.cache.stopArrivalDepartureCache",
            "org.transitclock.core.dataCache.ehcache.StopArrivalDepartureCache",
            "Specifies the class used to cache the arrival and departures for a stop.");

    private static StopArrivalDepartureCacheInterface singleton = null;

    @Bean
    public static StopArrivalDepartureCacheInterface getInstance() {

        if (singleton == null) {
            singleton = ClassInstantiator.instantiate(className.getValue(), StopArrivalDepartureCacheInterface.class);
        }

        return singleton;
    }
}
