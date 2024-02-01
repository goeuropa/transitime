/* (C)2023 */
package org.transitclock.core.predictiongenerator.datafilter;

import org.transitclock.annotations.Bean;
import org.transitclock.annotations.Configuration;
import org.transitclock.config.StringConfigValue;
import org.transitclock.utils.ClassInstantiator;

@Configuration
public class TravelTimeFilterFactory {
    private static TravelTimeDataFilter singleton = null;

    // The name of the class to instantiate
    private static final StringConfigValue className = new StringConfigValue(
            "transitclock.core.predictiongenerator.datafilter.traveltime",
            "org.transitclock.core.predictiongenerator.datafilter.TravelTimeDataFilterImpl",
            "Specifies the name of the class used to filter travel times.");

    @Bean
    public static TravelTimeDataFilter getInstance() {
        if (singleton == null) {
            singleton = ClassInstantiator.instantiate(className.getValue(), TravelTimeDataFilter.class);
        }
        return singleton;
    }
}
