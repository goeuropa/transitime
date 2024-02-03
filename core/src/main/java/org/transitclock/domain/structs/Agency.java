/* (C)2023 */
package org.transitclock.domain.structs;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.TimeZone;

import lombok.Data;
import lombok.Getter;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.transitclock.domain.hibernate.HibernateUtils;
import org.transitclock.gtfs.model.GtfsAgency;
import org.transitclock.utils.Time;

/**
 * Contains data from the agency.txt GTFS file. This class is for reading/writing that data to the
 * db.
 *
 * @author SkiBu Smith
 */
@Entity
@Data
@DynamicUpdate
@Table(name = "agencies")
public class Agency implements Serializable {

    @Column(name = "config_rev")
    @Id
    private final int configRev;

    @Column(name = "agency_name", length = 60)
    @Id
    private final String agencyName;

    // Note: this is the GTFS agency_id, not the usual
    // Transitime agencyId.
    @Column(name = "agency_id", length = 60)
    private final String agencyId;

    @Column(name = "agency_url")
    private final String agencyUrl;

    // Note: agencyTimezone can be reasonable long. At least as long
    // as "America/Los_Angeles". Valid timezone format is at
    // http://en.wikipedia.org/wiki/List_of_tz_zones
    @Column(name = "agency_timezone", length = 40)
    private final String agencyTimezone;

    @Column(name = "agency_lang", length = 15)
    private final String agencyLang;

    @Column(name = "agency_phone", length = 15)
    private final String agencyPhone;

    @Column(name = "agency_fare_url")
    private final String agencyFareUrl;

    @Embedded
    private final Extent extent;

    @Transient
    private TimeZone timezone = null;

    @Transient
    private Time time = null;

    /**
     * For creating object to be written to db.
     *
     * @param configRev
     * @param gtfsAgency
     * @param routes
     */
    public Agency(int configRev, GtfsAgency gtfsAgency, List<Route> routes) {
        this.configRev = configRev;
        this.agencyId = gtfsAgency.getAgencyId();
        this.agencyName = gtfsAgency.getAgencyName();
        this.agencyUrl = gtfsAgency.getAgencyUrl();
        this.agencyTimezone = gtfsAgency.getAgencyTimezone();
        this.agencyLang = gtfsAgency.getAgencyLang();
        this.agencyPhone = gtfsAgency.getAgencyPhone();
        this.agencyFareUrl = gtfsAgency.getAgencyFareUrl();

        Extent extent = new Extent();
        for (Route route : routes) {
            extent.add(route.getExtent());
        }
        this.extent = extent;
    }

    /** Needed because Hibernate requires no-arg constructor for reading in data */
    @SuppressWarnings("unused")
    protected Agency() {
        configRev = -1;
        agencyId = null;
        agencyName = null;
        agencyUrl = null;
        agencyTimezone = null;
        agencyLang = null;
        agencyPhone = null;
        agencyFareUrl = null;
        extent = null;
    }

    /**
     * Deletes rev from the Agencies table
     *
     * @param session
     * @param configRev
     * @return Number of rows deleted
     * @throws HibernateException
     */
    public static int deleteFromRev(Session session, int configRev) throws HibernateException {
        // Note that hql uses class name, not the table name
        return session.createMutationQuery("DELETE Agency WHERE configRev=" + configRev)
                .executeUpdate();
    }

    /**
     * Returns List of Agency objects for the specified database revision.
     *
     * @param session
     * @param configRev
     * @return
     * @throws HibernateException
     */
    public static List<Agency> getAgencies(Session session, int configRev) throws HibernateException {
        var query = session
                .createQuery("FROM Agency WHERE configRev = :configRev", Agency.class)
                .setParameter("configRev", configRev);
        return query.list();
    }

    /**
     * Returns the list of agencies for the specified project ID.
     *
     * @param agencyId Specifies name of database
     * @param configRev
     * @return
     */
    public static List<Agency> getAgencies(String agencyId, int configRev) {
        // Get the database session. This is supposed to be pretty light weight
        try (Session session = HibernateUtils.getSession(agencyId)) {
            return getAgencies(session, configRev);
        }
    }

    /**
     * Reads the current timezone for the agency from the agencies database
     *
     * @param agencyId
     * @return The TimeZone, or null if not successful
     */
    public static TimeZone getTimeZoneFromDb(String agencyId) {
        int configRev = ActiveRevisions.get(agencyId).getConfigRev();

        List<Agency> agencies = getAgencies(agencyId, configRev);
        if (!agencies.isEmpty()) {
            return agencies.get(0).getTimeZone();
        }

        return null;
    }

    /**
     * Returns cached TimeZone object for agency. Useful for creating Calendar objects and such.
     *
     * @return The TimeZone object for this agency
     */
    public TimeZone getTimeZone() {
        if (timezone == null) {
            timezone = TimeZone.getTimeZone(agencyTimezone);
        }
        return timezone;
    }

    /**
     * Returns cached Time object which allows one to easly convert epoch time to time of day and
     * such.
     *
     * @return Time object
     */
    public Time getTime() {
        if (time == null) {
            time = new Time(agencyTimezone);
        }
        return time;
    }

    /**
     * Note that this method returns the GTFS agency_id which is usually different from the
     * Transitime agencyId
     *
     * @return the agencyId
     */
    public String getId() {
        return agencyId;
    }

    /**
     * @return the agencyName
     */
    public String getName() {
        return agencyName;
    }

    /**
     * @return the agencyUrl
     */
    public String getUrl() {
        return agencyUrl;
    }

    /**
     * Valid timezone format is at http://en.wikipedia.org/wiki/List_of_tz_zones
     *
     * @return the agencyTimezone as a String
     */
    public String getTimeZoneStr() {
        return agencyTimezone;
    }

    /**
     * @return the agencyLang
     */
    public String getLang() {
        return agencyLang;
    }

    /**
     * @return the agencyPhone
     */
    public String getPhone() {
        return agencyPhone;
    }

    /**
     * @return the agencyFareUrl
     */
    public String getFareUrl() {
        return agencyFareUrl;
    }

}
