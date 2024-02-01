/* (C)2023 */
package org.transitclock.core.dataCache;

import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitclock.annotations.Component;
import org.transitclock.domain.structs.ArrivalDeparture;

/**
 * @author Sean Óg Crudden
 */
@Slf4j
@Component
public class ArrivalDeparturesToProcessHoldingTimesFor {

    private final ArrayList<ArrivalDeparture> m = new ArrayList<ArrivalDeparture>();

    public ArrivalDeparturesToProcessHoldingTimesFor() {}

    public void empty() {
        m.clear();
    }

    public void add(ArrivalDeparture ad) {
        m.add(ad);
    }

    public ArrayList<ArrivalDeparture> getList() {
        return m;
    }
}
