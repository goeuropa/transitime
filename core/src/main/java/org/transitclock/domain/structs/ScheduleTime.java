/* (C)2023 */
package org.transitclock.domain.structs;

import java.io.Serializable;

import com.querydsl.core.annotations.QueryEmbeddable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.transitclock.utils.Time;

/**
 * For keeping track of schedule times from GTFS data. Either arrival time or departure could be
 * null.
 *
 * @author SkiBu Smith
 */
@Data
@Embeddable
@QueryEmbeddable
public class ScheduleTime implements Serializable {

    // Times are in seconds. arrivalTime only set for last
    // stop in trip. Otherwise only departure time is set.
    /**
     * Time of day in seconds. Will be null if there is no arrival time (even if there is a
     * departure time). There will be no arrival time unless it is last stop in trip.
     */
    @Column(name = "arrival_time")
    private final Integer arrivalTime;

    /**
     * Time of day in seconds. Will be null if there is no departure time (even if there is an
     * arrival time). There will be no departure time if last stop of trip.
     */
    @Column(name = "departure_time")
    private final Integer departureTime;

    public ScheduleTime(Integer arrivalTime, Integer departureTime) {
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
    }

    protected ScheduleTime() {
        arrivalTime = null;
        departureTime = null;
    }

    /**
     * Returns departure time if there is one. Otherwise, returns arrival time if there is one.
     * Otherwise, returns null.
     *
     * @return
     */
    public Integer getTime() {
        if (departureTime != null)
            return departureTime;
        return arrivalTime;
    }

    @Override
    public String toString() {
        return "ScheduleTime ["
                + (arrivalTime != null ? "a=" + Time.timeOfDayStr(arrivalTime) : "")
                + (arrivalTime != null && departureTime != null ? ", " : "")
                + (departureTime != null ? "d=" + Time.timeOfDayStr(departureTime) : "")
                + "]";
    }
}
