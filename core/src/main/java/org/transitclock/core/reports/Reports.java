/* (C)2023 */
package org.transitclock.core.reports;

import org.json.JSONArray;
import org.json.JSONObject;
import org.transitclock.domain.webstructs.WebAgency;
import org.transitclock.utils.Time;

import java.text.ParseException;

public class Reports {

    private static final int MAX_ROWS = 50000;

    private static final int MAX_NUM_DAYS = 7;

    /**
     * Queries agency for AVL data and returns result as a JSON string. Limited to returning
     * MAX_ROWS (50,000) data points.
     *
     * @param agencyId
     * @param vehicleId Which vehicle to get data for. Set to null or empty string to get data for
     *     all vehicles
     * @param beginDate date to start query
     * @param numdays of days to collect data for
     * @param beginTime optional time of day during the date range
     * @param endTime optional time of day during the date range
     * @return AVL reports in JSON format. Can be empty JSON array if no data meets criteria.
     */
    public static String getAvlJson(
            String agencyId, String vehicleId, String beginDate, String numdays, String beginTime, String endTime) {
        // Determine the time portion of the SQL
        String timeSql = "";
        WebAgency agency = WebAgency.getCachedWebAgency(agencyId);
        // If beginTime or endTime set but not both then use default values
        if ((beginTime != null && !beginTime.isEmpty()) || (endTime != null && !endTime.isEmpty())) {
            if (beginTime == null || beginTime.isEmpty()) beginTime = "00:00";
            if (endTime == null || endTime.isEmpty()) endTime = "24:00";
        }
        // cast('2000-01-01 01:12:00'::timestamp as time);
        if (beginTime != null && !beginTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            if ("mysql".equals(agency.getDbType())) {
                timeSql = " AND time(time) BETWEEN '" + beginTime + "' AND '" + endTime + "' ";
            } else {
                timeSql = " AND cast(time::timestamp as time) BETWEEN '" + beginTime + "' AND '" + endTime + "' ";
            }
        }

        String sql = "";

        if ("mysql".equals(agency.getDbType())) {
            sql = "SELECT vehicle_id, name, time, assignment_id, lat, lon, speed, heading,"
                    + " time_processed, source FROM avl_reports INNER JOIN vehicle_configs ON"
                    + " vehicle_configs.id = avl_reports.vehicle_id WHERE time BETWEEN  cast(? as"
                    + " datetime) AND date_add(cast(? as datetime), INTERVAL "
                    + numdays
                    + " day) "
                    + timeSql;
        } else {
            sql = "SELECT vehicle_id, name, time, assignment_id, lat, lon, speed, heading,"
                    + " time_processed, source FROM avl_reports INNER JOIN vehicle_configs ON"
                    + " vehicle_configs.id = avl_reports.vehicle_id WHERE time BETWEEN  cast(? as"
                    + " timestamp) AND cast(? as timestamp) + INTERVAL '"
                    + numdays
                    + " day' "
                    + timeSql;
        }

        // If only want data for single vehicle then specify so in SQL
        if (vehicleId != null && !vehicleId.isEmpty()) sql += " AND vehicle_id='" + vehicleId + "' ";

        // Make sure data is ordered by vehicleId so that can draw lines
        // connecting the AVL reports per vehicle properly. Also then need
        // to order by time to make sure they are in proper order. And
        // lastly, limit AVL reports to 5000 so that someone doesn't try
        // to view too much data at once.

        sql += "ORDER BY vehicle_id, time LIMIT " + MAX_ROWS;

        String json = null;
        try {
            java.util.Date startdate = Time.parseDate(beginDate);

            json = GenericJsonQuery.getJsonString(agencyId, sql, startdate, startdate);

        } catch (ParseException e) {
            json = e.getMessage();
        }

        return json;
    }

    public static String getTripsFromArrivalAndDeparturesByDate(String agencyId, String date) {
        // postgresql only, should throw error if it's other database type
        String sql = "SELECT "
                + "	arrivals_departures.trip_id as tripId "
                + "FROM arrivals_departures "
                + "WHERE Date(arrivals_departures.time) = DATE('" + date + "') "
                + "GROUP BY arrivals_departures.trip_id";

        String json = null;
        json = GenericJsonQuery.getJsonString(agencyId, sql);

        return json;
    }

    public static String getTripWithTravelTimes(String agencyId, String tripId, String date) {
        // postgresql only, should throw error if it's other database type
        String sql = "SELECT"
                + "	arrivals_departures.trip_id as tripId"
                + "	,arrivals_departures.direction_id as directionId"
                + "	,arrivals_departures.stop_id as stopId"
                + "	,stops.code as stopCode"
                + "	,stops.name as stopName"
                + "	,stops.lat as lat"
                + "	,stops.lon as lon"
                + "	,arrivals_departures.stop_order as stopOrder"
                + "	,arrivals_departures.vehicle_id as vehicleId"
                + "	,vehicle_configs.name as vehicleName"
                + "	,arrivals_departures.time as arrivalTime"
                + "	,ADDeparture.time as departureTime"
                + "	,CASE WHEN ADDeparture.scheduled_time ISNULL"
                + "	 THEN DATE('"
                + date
                + "') + trip_scheduled_times_list.arrival_time * interval '1 second'"
                + "	 ELSE ADDeparture.scheduled_time"
                + "	 END AS scheduledTime"
                + "	,CASE WHEN ADDeparture.scheduled_time ISNULL"
                + "	 THEN regexp_replace(CAST(DATE_TRUNC('second', DATE('"
                + date
                + "') + trip_scheduled_times_list.arrival_time * interval '1 second') -"
                + " DATE_TRUNC('second', arrivals_departures.time::timestamp) AS VARCHAR),"
                + " '^00:', '')      ELSE regexp_replace(CAST(DATE_TRUNC('second',"
                + " ADDeparture.scheduled_time::timestamp) - DATE_TRUNC('second',"
                + " ADDeparture.time::timestamp) AS VARCHAR), '^00:', '') 	 END AS"
                + " difference_in_seconds	FROM arrivals_departures LEFT JOIN stops ON"
                + " stops.id = arrivals_departures.stop_id 	and stops.config_rev ="
                + " arrivals_departures.config_rev LEFT JOIN vehicle_configs on"
                + " vehicle_configs.id = arrivals_departures.vehicle_id LEFT JOIN"
                + " LATERAL(SELECT * from arrivals_departures ad where ad.trip_id ="
                + " arrivals_departures.trip_id 	AND ad.direction_id ="
                + " arrivals_departures.direction_id 	AND ad.stop_id ="
                + " arrivals_departures.stop_id 	AND LOWER(ad.type) = LOWER('DEPARTURE') 	AND"
                + " DATE(ad.avl_time) = DATE(arrivals_departures.avl_time) 	AND ad.time >="
                + " arrivals_departures.time 	ORDER BY ad.time ASC LIMIT 1) ADDeparture ON"
                + " True LEFT JOIN trip_scheduled_times_list ON"
                + " trip_scheduled_times_list.trip_trip_id = arrivals_departures.trip_id 	AND"
                + " trip_scheduled_times_list.trip_config_rev = arrivals_departures.config_rev "
                + "	AND trip_scheduled_times_list.list_index ="
                + " arrivals_departures.stop_order WHERE arrivals_departures.trip_id = '"
                + tripId
                + "' and arrivals_departures.is_arrival = 'True' and"
                + " Date(arrivals_departures.time) = DATE('"
                + date
                + "')ORDER BY arrivals_departures.time asc, arrivals_departures.direction_id"
                + " asc, arrivals_departures.gtfs_stop_seq asc";

        String json = null;
        json = GenericJsonQuery.getJsonString(agencyId, sql, date, date, tripId, date);

        return json;
    }

    public static String getTripsWithTravelTimes(String agencyId, String date) {
        String sql = "SELECT"
                + "	arrivals_departures.trip_id as tripId"
                + "	,arrivals_departures.direction_id as directionId"
                + "	,arrivals_departures.stop_id as stopId"
                + "	,stops.code as stopCode"
                + "	,stops.name as stopName"
                + "	,stops.lat as lat"
                + "	,stops.lon as lon"
                + "	,arrivals_departures.stop_order as stopOrder"
                + "	,arrivals_departures.vehicle_id as vehicleId"
                + "	,vehicle_configs.name as vehicleName"
                + "	,arrivals_departures.time as arrivalTime"
                + "	,ADDeparture.time as departureTime"
                + "	,CASE WHEN ADDeparture.scheduled_time ISNULL"
                + "	 THEN DATE('"
                + date
                + "') + trip_scheduled_times_list.arrival_time * interval '1 second'"
                + "	 ELSE ADDeparture.scheduled_time"
                + "	 END AS scheduledTime"
                + "	,CASE WHEN ADDeparture.scheduled_time ISNULL"
                + "	 THEN regexp_replace(CAST(DATE_TRUNC('second', DATE('"
                + date
                + "') + trip_scheduled_times_list.arrival_time * interval '1 second') -"
                + " DATE_TRUNC('second', arrivals_departures.time::timestamp) AS VARCHAR),"
                + " '^00:', '')      ELSE regexp_replace(CAST(DATE_TRUNC('second',"
                + " ADDeparture.scheduled_time::timestamp) - DATE_TRUNC('second',"
                + " ADDeparture.time::timestamp) AS VARCHAR), '^00:', '') 	 END AS"
                + " difference_in_seconds	FROM arrivals_departures LEFT JOIN stops ON"
                + " stops.id = arrivals_departures.stop_id 	and stops.config_rev ="
                + " arrivals_departures.config_rev LEFT JOIN vehicle_configs on"
                + " vehicle_configs.id = arrivals_departures.vehicle_id LEFT JOIN"
                + " LATERAL(SELECT * from arrivals_departures ad where ad.trip_id ="
                + " arrivals_departures.trip_id 	AND ad.direction_id ="
                + " arrivals_departures.direction_id 	AND ad.stop_id ="
                + " arrivals_departures.stop_id 	AND LOWER(ad.type) = LOWER('DEPARTURE') 	AND"
                + " DATE(ad.avl_time) = DATE(arrivals_departures.avl_time) 	AND ad.time >="
                + " arrivals_departures.time 	ORDER BY ad.time ASC LIMIT 1) ADDeparture ON"
                + " True LEFT JOIN trip_scheduled_times_list ON"
                + " trip_scheduled_times_list.trip_trip_id = arrivals_departures.trip_id 	AND"
                + " trip_scheduled_times_list.trip_config_rev = arrivals_departures.config_rev "
                + "	AND trip_scheduled_times_list.list_index ="
                + " arrivals_departures.stop_order WHERE arrivals_departures.is_arrival ="
                + " 'True' and Date(arrivals_departures.time) = DATE('"
                + date
                + "')ORDER BY arrivals_departures.trip_id asc, arrivals_departures.gtfs_stop_seq"
                + " asc, arrivals_departures.time asc";

        String json = null;
        json = GenericJsonQuery.getJsonString(agencyId, sql, date, date, date);

        return json;
    }

    /* Provides schedule adherence data in JSON format. Provides for
      the specified route the number arrivals/departures that
      are early, number late, number on time, and number total for each
      direction for each stop.
      Request parameters are:
        a - agency ID
        r - route ID or route short name.
        dateRange - in format "xx/xx/xx to yy/yy/yy"
        beginDate - date to begin query. For if dateRange not used.
        numDays - number of days can do query. Limited to 31 days. For if dateRange not used.
        beginTime - for optionally specifying time of day for query for each day
        endTime - for optionally specifying time of day for query for each day
        allowableEarlyMinutes - how early vehicle can be and still be OK.  Decimal format OK.
        allowableLateMinutes - how early vehicle can be and still be OK. Decimal format OK.
    */
    public static String getScheduleAdhByStops(
            String agencyId,
            String route,
            String beginDate,
            String allowableEarly,
            String allowableLate,
            String beginTime,
            String endTime,
            int numDays) {
        if (allowableEarly == null || allowableEarly.isEmpty()) allowableEarly = "1.0";
        String allowableEarlyMinutesStr = "'" + SqlUtils.convertMinutesToSecs(allowableEarly) + " seconds'";

        if (allowableLate == null || allowableLate.isEmpty()) allowableLate = "4.0";
        String allowableLateMinutesStr = "'" + SqlUtils.convertMinutesToSecs(allowableLate) + " seconds'";

        String sql = "WITH trips_early_query_with_time AS ( SELECT trip_id AS trips_early, 	"
                + " regexp_replace(CAST(DATE_TRUNC('second', ad.scheduled_time::timestamp) -"
                + " DATE_TRUNC('second', ad.time::timestamp) AS VARCHAR), '^00:', '')"
                + " difference_in_seconds, \n"
                // + "	 abs(((ad.time / 1000) - (ad.scheduled_time / 1000))) AS
                // difference_in_seconds,  \n"
                + "	 s.id AS stop_id, \n"
                + "	 ad.stop_order AS stop_order, \n"
                + "	 ad.vehicle_id AS v_id \n"  //
                + " 	FROM arrivals_departures ad, stops s  \n"
                + "WHERE "
                // To get stop name
                + " ad.config_rev = s.config_rev \n"
                + " AND ad.stop_id = s.id \n"
                // Only need arrivals/departures that have a schedule time
                + " AND ad.scheduled_time IS NOT NULL \n"
                // Specifies which routes to provide data for
                + SqlUtils.routeClause(route, "ad")
                + "\n"
                + SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate)
                + "\n"
                + " AND scheduled_time-time > "
                + allowableEarlyMinutesStr
                + " \n"
                + "	 ORDER BY direction_id, ad.stop_order, s.name \n"
                + "), \n"
                + "trips_late_query_with_time AS ( SELECT trip_id AS trips_late,  	"
                + " regexp_replace(CAST(DATE_TRUNC('second', ad.time::timestamp) -"
                + " DATE_TRUNC('second', ad.scheduled_time::timestamp) AS VARCHAR), '^00:',"
                + " '') difference_in_seconds, \n"
                // + "	 ((ad.time / 1000) - (ad.scheduled_time / 1000)) AS
                // difference_in_seconds,  \n"
                + "	 s.id AS stop_id, \n"
                + "	 ad.stop_order AS stop_order, \n"
                + "	 ad.vehicle_id AS v_id \n"  //
                + "	FROM arrivals_departures ad, Stops s  \n"
                + "WHERE "
                // To get stop name
                + " ad.config_rev = s.config_rev \n"
                + " AND ad.stop_id = s.id \n"
                // Only need arrivals/departures that have a schedule time
                + " AND ad.scheduled_time IS NOT NULL \n"
                // Specifies which routes to provide data for
                + SqlUtils.routeClause(route, "ad")
                + "\n"
                + SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate)
                + "\n"
                + " AND time-scheduled_time > "
                + allowableLateMinutesStr
                + " \n"
                + "	 ORDER BY direction_id, ad.stop_order, s.name \n"
                + "), \n"
                + "trips_late_query_v2 AS ( SELECT \n"
                + " array_to_string(array_agg('trip: ' || trips_late::text || ' (' ||"
                + " difference_in_seconds::text || '), vehicle: ' || v_id::text order by trips_late::text), '; ')"
                + " AS trips_late,   \n"
                + "		 stop_id,  \n"
                + "		 stop_order  \n"
                + "	 	FROM trips_late_query_with_time \n"
                + "		 GROUP BY stop_id, stop_order \n"
                + "	), \n"
                + "	trips_early_query_v2 AS ( SELECT \n"
                + "	array_to_string(array_agg('trip: ' || trips_early::text || ' (' ||"
                + " difference_in_seconds::text || '), vehicle: ' || v_id::text order by trips_early::text), '; ')"
                + " AS trips_early,  \n"
                + "		 stop_id,  \n"
                + "		 stop_order  \n"
                + "	 	FROM trips_early_query_with_time \n"
                + "		 GROUP BY stop_id, stop_order \n"
                + "	) \n"
                + "SELECT      COUNT(CASE WHEN scheduled_time-time > "
                + allowableEarlyMinutesStr
                + " THEN 1 ELSE null END) as early, \n"
                + "     COUNT(CASE WHEN scheduled_time-time <= "
                + allowableEarlyMinutesStr
                + " AND time-scheduled_time <= "
                + allowableLateMinutesStr
                + " THEN 1 ELSE null END) AS ontime, \n"
                + "     COUNT(CASE WHEN time-scheduled_time > "
                + allowableLateMinutesStr
                + " THEN 1 ELSE null END) AS late, \n"
                + "     COUNT(*) AS total, \n"
                + "     s.name AS stop_name, \n"
                + "     ad.direction_id AS direction_id, \n"
                + " 	trips_early_query_v2.trips_early as trips_early, \n"
                + " 	trips_late_query_v2.trips_late as trips_late  \n"
                + "FROM arrivals_departures ad	INNER JOIN Stops s ON ad.stop_id = s.id \n"
                + "	LEFT JOIN trips_early_query_v2 ON s.id = trips_early_query_v2.stop_id"
                + " AND ad.stop_order = trips_early_query_v2.stop_order \n"
                + "	LEFT JOIN trips_late_query_v2 ON s.id = trips_late_query_v2.stop_id AND"
                + " ad.stop_order = trips_late_query_v2.stop_order \n"
                + "WHERE "
                // To get stop name
                + " ad.config_rev = s.config_rev \n"
                + " AND ad.stop_id = s.id \n"
                // Only need arrivals/departures that have a schedule time
                + " AND ad.scheduled_time IS NOT NULL \n"
                // Specifies which routes to provide data for
                + SqlUtils.routeClause(route, "ad")
                + "\n"
                + SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate)
                + "\n"
                // Grouping and ordering is a bit complicated since might also be looking
                // at old arrival/departure data that doen't have stoporder defined. Also,
                // when configuration changes happen then the stop order can change.
                // Therefore want to group by directionId and stop name. Need to also
                // group by stop order so that can output it, which can be useful for
                // debugging, plus need to order by stop order. For the ORDER BY clause
                // need to order by direction id and stop order, but also the stop name
                // as a backup for if stoporder not defined for data and is therefore
                // always the same and doesn't provide any ordering info.
                + " GROUP BY direction_id, s.name, s.id, ad.stop_order,"
                + " trips_early_query_v2.trips_early, trips_late_query_v2.trips_late \n"
                + " ORDER BY direction_id, ad.stop_order, s.name";

        /*+ "WITH trips_early_query AS ( SELECT "
        + "	 array_to_string(array_agg(distinct tripid::text order by tripid::text), '; ') AS trips_early, \n"
        + "	 s.id AS stop_id, \n"
        + "	 ad.stopOrder AS stop_order \n"
        + " 	FROM ArrivalsDepartures ad, Stops s  \n"
        		+ "WHERE "
        	    // To get stop name
        	    + " ad.config_rev = s.config_rev \n"
        	    + " AND ad.stop_id = s.id \n"
        	    // Only need arrivals/departures that have a schedule time
        	    + " AND ad.scheduled_time IS NOT NULL \n"
        	    // Specifies which routes to provide data for
        	    + SqlUtils.routeClause(route, "ad") + "\n"
        	    + SqlUtils.timeRangeClause(agencyId, "ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate) + "\n"
        	    + " AND scheduled_time-time > " + allowableEarlyMinutesStr + " \n"
        + "	 GROUP BY directionid, s.name, s.id, ad.stopOrder \n"
        + "	 ORDER BY directionid, ad.stopOrder, s.name \n"
        + "), \n"

        + "trips_late_query AS ( SELECT "
        + "	 array_to_string(array_agg(distinct tripid::text order by tripid::text), '; ') AS trips_late, \n"
        + "	 s.id AS stop_id, \n"
        + "	 ad.stopOrder AS stop_order \n"
        + "	FROM ArrivalsDepartures ad, Stops s  \n"
        		+ "WHERE "
        	    // To get stop name
        	    + " ad.config_rev = s.config_rev \n"
        	    + " AND ad.stop_id = s.id \n"
        	    // Only need arrivals/departures that have a schedule time
        	    + " AND ad.scheduled_time IS NOT NULL \n"
        	    // Specifies which routes to provide data for
        	    + SqlUtils.routeClause(route, "ad") + "\n"
        	    + SqlUtils.timeRangeClause(agencyId, "ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate) + "\n"
        	    + " AND time-scheduled_time > " + allowableLateMinutesStr + " \n"
        + "	 GROUP BY directionid, s.name, s.id, ad.stopOrder \n"
        + "	 ORDER BY directionid, ad.stopOrder, s.name \n"
        + ") \n"
        + "SELECT "
        + "     COUNT(CASE WHEN scheduled_time-time > " + allowableEarlyMinutesStr + " THEN 1 ELSE null END) as early, \n"
        + "     COUNT(CASE WHEN scheduled_time-time <= " + allowableEarlyMinutesStr + " AND time-scheduled_time <= "
        			+ allowableLateMinutesStr + " THEN 1 ELSE null END) AS ontime, \n"
          + "     COUNT(CASE WHEN time-scheduled_time > " + allowableLateMinutesStr + " THEN 1 ELSE null END) AS late, \n"
          + "     COUNT(*) AS total, \n"
          + "     s.name AS stop_name, \n"
          + "     ad.directionid AS direction_id, \n"
          + " 	trips_early_query.trips_early as trips_early, \n"
          + " 	trips_late_query.trips_late as trips_late \n"
          + "FROM ArrivalsDepartures ad"
          + "	INNER JOIN Stops s ON ad.stop_id = s.id \n"
          + "	LEFT JOIN trips_early_query ON s.id = trips_early_query.stop_id AND ad.stopOrder = trips_early_query.stop_order \n"
          + "	LEFT JOIN trips_late_query ON s.id = trips_late_query.stop_id AND ad.stopOrder = trips_late_query.stop_order \n"
          + "WHERE "
          // To get stop name
          + " ad.config_rev = s.config_rev \n"
          + " AND ad.stop_id = s.id \n"
          // Only need arrivals/departures that have a schedule time
          + " AND ad.scheduled_time IS NOT NULL \n"
          // Specifies which routes to provide data for
          + SqlUtils.routeClause(route, "ad") + "\n"
          + SqlUtils.timeRangeClause(agencyId, "ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate) + "\n"
          // Grouping and ordering is a bit complicated since might also be looking
          // at old arrival/departure data that doen't have stoporder defined. Also,
          // when configuration changes happen then the stop order can change.
          // Therefore want to group by directionId and stop name. Need to also
          // group by stop order so that can output it, which can be useful for
          // debugging, plus need to order by stop order. For the ORDER BY clause
          // need to order by direction id and stop order, but also the stop name
          // as a backup for if stoporder not defined for data and is therefore
          // always the same and doesn't provide any ordering info.
          + " GROUP BY directionid, s.name, s.id, ad.stopOrder, trips_early_query.trips_early, trips_late_query.trips_late \n"
          + " ORDER BY directionid, ad.stopOrder, s.name";*/

        // Do the query and return result in JSON format
        String jsonString = "";
        jsonString = GenericJsonQuery.getJsonString(agencyId, sql);
        return jsonString;
    }

    /* Provides schedule adherence data in JSON format. Provides for
      the specified route the number arrivals/departures that
      are early, number late, number on time, and number total for each
      direction for each stop.
      Request parameters are:
        a - agency ID
        r - route ID or route short name.
        dateRange - in format "xx/xx/xx to yy/yy/yy"
        beginDate - date to begin query. For if dateRange not used.
        numDays - number of days can do query. Limited to 31 days. For if dateRange not used.
        beginTime - for optionally specifying time of day for query for each day
        endTime - for optionally specifying time of day for query for each day
        allowableEarlyMinutes - how early vehicle can be and still be OK.  Decimal format OK.
        allowableLateMinutes - how early vehicle can be and still be OK. Decimal format OK.
    */
    public static String getScheduleAdhByStops_v2(
            String agencyId,
            String route,
            String beginDate,
            String allowableEarly,
            String allowableLate,
            String beginTime,
            String endTime,
            int numDays) {
        if (allowableEarly == null || allowableEarly.isEmpty()) allowableEarly = "1.0";
        String allowableEarlyMinutesStr = "'" + SqlUtils.convertMinutesToSecs(allowableEarly) + " seconds'";

        if (allowableLate == null || allowableLate.isEmpty()) allowableLate = "4.0";
        String allowableLateMinutesStr = "'" + SqlUtils.convertMinutesToSecs(allowableLate) + " seconds'";

        String sql = "WITH trips_early_query_with_time AS ( SELECT trip_id AS trips_early, 	"
                + " regexp_replace(CAST(DATE_TRUNC('second', ad.scheduled_time::timestamp) -"
                + " DATE_TRUNC('second', ad.time::timestamp) AS VARCHAR), '^00:', '')"
                + " difference_in_seconds, \n"
                // + "	 abs(((ad.time / 1000) - (ad.scheduled_time / 1000))) AS
                // difference_in_seconds,  \n"
                + "	 s.id AS stop_id, \n"
                + "	 ad.stop_order AS stop_order \n"
                + " 	FROM arrivals_departures ad, stops s  \n"
                + "WHERE "
                // To get stop name
                + " ad.config_rev = s.config_rev \n"
                + " AND ad.stop_id = s.id \n"
                // Only need arrivals/departures that have a schedule time
                + " AND ad.scheduled_time IS NOT NULL \n"
                // Specifies which routes to provide data for
                + SqlUtils.routeClause(route, "ad")
                + "\n"
                + SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate)
                + "\n"
                + " AND scheduled_time-time > "
                + allowableEarlyMinutesStr
                + " \n"
                + "	 ORDER BY direction_id, ad.stop_order, s.name \n"
                + "), \n"
                + "trips_late_query_with_time AS ( SELECT trip_id AS trips_late,  	"
                + " regexp_replace(CAST(DATE_TRUNC('second', ad.time::timestamp) -"
                + " DATE_TRUNC('second', ad.scheduled_time::timestamp) AS VARCHAR), '^00:',"
                + " '') difference_in_seconds, \n"
                // + "	 ((ad.time / 1000) - (ad.scheduled_time / 1000)) AS
                // difference_in_seconds,  \n"
                + "	 s.id AS stop_id, \n"
                + "	 ad.stop_order AS stop_order \n"
                + "	FROM arrivals_departures ad, stops s  \n"
                + "WHERE "
                // To get stop name
                + " ad.config_rev = s.config_rev \n"
                + " AND ad.stop_id = s.id \n"
                // Only need arrivals/departures that have a schedule time
                + " AND ad.scheduled_time IS NOT NULL \n"
                // Specifies which routes to provide data for
                + SqlUtils.routeClause(route, "ad")
                + "\n"
                + SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate)
                + "\n"
                + " AND time-scheduled_time > "
                + allowableLateMinutesStr
                + " \n"
                + "	 ORDER BY direction_id, ad.stop_order, s.name \n"
                + "), \n"
                + "trips_late_query_v2 AS ( 		SELECT"
                + " array_to_string(array_agg(trips_late::text || ' (' ||"
                + " difference_in_seconds::text || ')' order by trips_late::text), '; ') AS"
                + " trips_late,   \n"
                + "		 stop_id,  \n"
                + "		 stop_order  \n"
                + "	 	FROM trips_late_query_with_time \n"
                + "		 GROUP BY stop_id, stop_order \n"
                + "	), \n"
                + "	trips_early_query_v2 AS (  \n"
                + "		SELECT array_to_string(array_agg(trips_early::text || ' (' ||"
                + " difference_in_seconds::text || ')' order by trips_early::text), '; ')"
                + " AS trips_early,  \n"
                + "		 stop_id,  \n"
                + "		 stop_order  \n"
                + "	 	FROM trips_early_query_with_time \n"
                + "		 GROUP BY stop_id, stop_order \n"
                + "	) \n"
                + "SELECT      COUNT(CASE WHEN scheduled_time-time > "
                + allowableEarlyMinutesStr
                + " THEN 1 ELSE null END) as early, \n"
                + "     COUNT(CASE WHEN scheduled_time-time <= "
                + allowableEarlyMinutesStr
                + " AND time-scheduled_time <= "
                + allowableLateMinutesStr
                + " THEN 1 ELSE null END) AS ontime, \n"
                + "     COUNT(CASE WHEN time-scheduled_time > "
                + allowableLateMinutesStr
                + " THEN 1 ELSE null END) AS late, \n"
                + "     COUNT(*) AS total, \n"
                + "     s.name AS stop_name, \n"
                + "     s.id AS stop_id, \n"
                + "     ad.direction_id AS direction_id, \n"
                + " 	trips_early_query_v2.trips_early as trips_early, \n"
                + " 	trips_late_query_v2.trips_late as trips_late  \n"
                + "FROM ArrivalsDepartures ad	INNER JOIN Stops s ON ad.stop_id = s.id \n"
                + "	LEFT JOIN trips_early_query_v2 ON s.id = trips_early_query_v2.stop_id"
                + " AND ad.stop_order = trips_early_query_v2.stop_order \n"
                + "	LEFT JOIN trips_late_query_v2 ON s.id = trips_late_query_v2.stop_id AND"
                + " ad.stop_order = trips_late_query_v2.stop_order \n"
                + "WHERE "
                // To get stop name
                + " ad.config_rev = s.config_rev \n"
                + " AND ad.stop_id = s.id \n"
                // Only need arrivals/departures that have a schedule time
                + " AND ad.scheduled_time IS NOT NULL \n"
                // Specifies which routes to provide data for
                + SqlUtils.routeClause(route, "ad")
                + "\n"
                + SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate)
                + "\n"
                // Grouping and ordering is a bit complicated since might also be looking
                // at old arrival/departure data that doen't have stoporder defined. Also,
                // when configuration changes happen then the stop order can change.
                // Therefore want to group by directionId and stop name. Need to also
                // group by stop order so that can output it, which can be useful for
                // debugging, plus need to order by stop order. For the ORDER BY clause
                // need to order by direction id and stop order, but also the stop name
                // as a backup for if stoporder not defined for data and is therefore
                // always the same and doesn't provide any ordering info.
                + " GROUP BY direction_id, s.name, s.id, ad.stop_order,"
                + " trips_early_query_v2.trips_early, trips_late_query_v2.trips_late \n"
                + " ORDER BY direction_id, ad.stop_order, s.name";

        String sql_trips_early = "SELECT trip_id AS trips_early, 	 regexp_replace(CAST(DATE_TRUNC('second',"
                + " ad.scheduled_time::timestamp) - DATE_TRUNC('second', ad.time::timestamp) AS"
                + " VARCHAR), '^00:', '') difference_in_seconds, \n"
                + "	 s.id AS stop_id, \n"
                + "	 ad.stop_order AS stop_order \n"
                + " 	FROM arrivals_departures ad, stops s  \n"
                + "WHERE "
                // To get stop name
                + " ad.config_rev = s.config_rev \n"
                + " AND ad.stop_id = s.id \n"
                // Only need arrivals/departures that have a schedule time
                + " AND ad.scheduled_time IS NOT NULL \n"
                // Specifies which routes to provide data for
                + SqlUtils.routeClause(route, "ad")
                + "\n"
                + SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate)
                + "\n"
                + " AND scheduled_time-time > "
                + allowableEarlyMinutesStr
                + " \n"
                + "	 ORDER BY direction_id, ad.stop_order, s.name \n";

        String sql_trips_late = "SELECT trip_id AS trips_late,  	 regexp_replace(CAST(DATE_TRUNC('second',"
                + " ad.time::timestamp) - DATE_TRUNC('second', ad.scheduled_time::timestamp) AS"
                + " VARCHAR), '^00:', '') difference_in_seconds, \n"
                // + "	 ((ad.time / 1000) - (ad.scheduled_time / 1000)) AS
                // difference_in_seconds,  \n"
                + "	 s.id AS stop_id, \n"
                + "	 ad.stop_order AS stop_order \n"
                + "	FROM arrivals_departures ad, stops s  \n"
                + "WHERE "
                // To get stop name
                + " ad.config_rev = s.config_rev \n"
                + " AND ad.stop_id = s.id \n"
                // Only need arrivals/departures that have a schedule time
                + " AND ad.scheduled_time IS NOT NULL \n"
                // Specifies which routes to provide data for
                + SqlUtils.routeClause(route, "ad")
                + "\n"
                + SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate)
                + "\n"
                + " AND time-scheduled_time > "
                + allowableLateMinutesStr
                + " \n"
                + "	 ORDER BY direction_id, ad.stop_order, s.name \n";
        // Do the query and return result in JSON format
        String jsonStringTripsEarly = GenericJsonQuery.getJsonString(agencyId, sql_trips_early);
        String jsonStringTripsLate = GenericJsonQuery.getJsonString(agencyId, sql_trips_late);

        JSONObject tripsLateObject = new JSONObject(jsonStringTripsLate);
        JSONArray tripsLateJsonArray = tripsLateObject.getJSONArray("data");
        JSONObject tripsEarlyObject = new JSONObject(jsonStringTripsEarly);
        JSONArray tripsEarlyJsonArray = tripsEarlyObject.getJSONArray("data");
        // {"early":2,"ontime":0,"late":0,"total":2,"stop_name":"Os. Czwartaków",
        // "direction_id":"0","trips_early":"400-1-R-0:30; 400-2-R-2:45"},

        String jsonString = "";
        jsonString = GenericJsonQuery.getJsonString(agencyId, sql);
        return jsonString;
    }


    /**
     * Queries agency for Stop ID and returns result as a JSON string. Limited to returning
     * MAX_ROWS (50,000) data points.
     *
     * @return Stop reports in JSON format. Can be empty JSON array if no data meets criteria.
     */
    public static String getReportForStopById (String agencyId,
                                        String stop,
                                        String beginDate,
                                        String allowableEarly,
                                        String allowableLate,
                                        String beginTime,
                                        String endTime,
                                        int numDays) {
        if (allowableEarly == null || allowableEarly.isEmpty()) allowableEarly = "1.0";
        String allowableEarlyMinutesStr = "'" + SqlUtils.convertMinutesToSecs(allowableEarly) + " seconds'";

        if (allowableLate == null || allowableLate.isEmpty()) allowableLate = "4.0";
        String allowableLateMinutesStr = "'" + SqlUtils.convertMinutesToSecs(allowableLate) + " seconds'";

        String sql = " WITH early AS (SELECT time                   AS early,\n" +
                "                      s.name                       AS name,\n" +
                "                      ad.route_id                  AS route,\n" +
                "                      ad.trip_id                   AS trip,\n" +
                "                      ad.block_id                  AS block,\n" +
                "                      ad.vehicle_id                AS vehicle,\n" +
                "                      ad.scheduled_time            AS schedule,\n" +
                "                      regexp_replace(CAST(DATE_TRUNC('second', ad.scheduled_time::timestamp) - " +
                "                                          DATE_TRUNC('second', ad.time::timestamp) AS VARCHAR),\n" +
                "                              '^00:', ''\n" +
                "                      )                            AS difference\n" +
                "               FROM arrivals_departures ad,\n" +
                "                    stops s\n" +
                "               WHERE ad.config_rev = s.config_rev\n" +
                "                 AND ad.stop_id = s.id\n" +
                "                 AND ad.scheduled_time IS NOT NULL \n" +
//              Specifies which stops to provide data for
                SqlUtils.stopClause(stop, "ad") +
                " \n" +
//              Defines time range
                SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate) +
                " \n" +
                "                 AND scheduled_time - time > " + allowableEarlyMinutesStr +
                " \n" +
                "               ORDER BY early),\n" +
                "     on_time AS (SELECT time                        AS on_time,\n" +
                "                       s.name                       AS name,\n" +
                "                       ad.route_id                  AS route,\n" +
                "                       ad.trip_id                   AS trip,\n" +
                "                       ad.block_id                  AS block,\n" +
                "                       ad.vehicle_id                AS vehicle,\n" +
                "                       ad.scheduled_time            AS schedule,\n" +
                "                       regexp_replace(\n" +
                "                               CAST(DATE_TRUNC('second', ad.scheduled_time::timestamp) - " +
                "                                    DATE_TRUNC('second', ad.time::timestamp) AS VARCHAR),\n" +
                "                               '^(-)?00:', '\\1'\n" +
                "                       )                            AS difference\n" +
                "                FROM arrivals_departures ad,\n" +
                "                     stops s\n" +
                "                WHERE ad.config_rev = s.config_rev\n" +
                "                  AND ad.stop_id = s.id\n" +
                "                  AND ad.scheduled_time IS NOT NULL \n" +
//              Specifies which stops to provide data for
                SqlUtils.stopClause(stop, "ad") +
                " \n" +
//              Defines time range
                SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate) +
                " \n" +
                "                  AND scheduled_time - time <= " + allowableEarlyMinutesStr +
                " \n" +
                "                  AND time - scheduled_time <= " + allowableLateMinutesStr +
                " \n" +
                "                ORDER BY on_time),\n" +
                "     late AS (SELECT time                         AS late,\n" +
                "                     s.name                       AS name,\n" +
                "                     ad.route_id                  AS route,\n" +
                "                     ad.trip_id                   AS trip,\n" +
                "                     ad.block_id                  AS block,\n" +
                "                     ad.vehicle_id                AS vehicle,\n" +
                "                     ad.scheduled_time            AS schedule,\n" +
                "                     regexp_replace(\n" +
                "                             CAST(DATE_TRUNC('second', ad.scheduled_time::timestamp) - " +
                "                                  DATE_TRUNC('second', ad.time::timestamp) AS VARCHAR),\n" +
                "                             '^(-)00:', '\\1'\n" +
                "                     )                            AS difference\n" +
                "              FROM arrivals_departures ad,\n" +
                "                   stops s\n" +
                "              WHERE ad.config_rev = s.config_rev\n" +
                "                AND ad.stop_id = s.id\n" +
                "                AND ad.scheduled_time IS NOT NULL \n" +
//              Specifies which stops to provide data for
                SqlUtils.stopClause(stop, "ad") +
                " \n" +
//              Defines time range
                SqlUtils.timeRangeClause("ad.time", MAX_NUM_DAYS, numDays, beginTime, endTime, beginDate) +
                " \n" +
                "               AND time - scheduled_time > " + allowableLateMinutesStr +
                " \n" +
                "              ORDER BY late) \n" +
                "SELECT * FROM \n" +
                "     early\n" +
                "         FULL OUTER JOIN\n" +
                "     on_time ON early.early = on_time.on_time\n" +
                "         FULL OUTER JOIN\n" +
                "     late ON on_time.on_time = late.late\n";

        return GenericJsonQuery.getJsonString(agencyId, sql).replaceFirst("\\bdata\\b", stop);
    }

    /**
     * Queries agency for AVL data and returns result as a JSON string. Limited to returning
     * MAX_ROWS (50,000) data points.
     *
     * @return Last AVL reports in JSON format. Can be empty JSON array if no data meets criteria.
     */
    public static String getLastAvlJson(String agencyId) {
        WebAgency agency = WebAgency.getCachedWebAgency(agencyId);
        String sql = "";
        if (agency.getDbType().equals("mysql")) {
            sql = "SELECT a.vehicle_id, vC.name, maxTime, lat, lon "
                    + "FROM "
                    + "(SELECT vehicle_id, max(time) AS maxTime "
                    + "FROM avl_reports WHERE time > date_sub(now(), interval 1 day) "
                    + "GROUP BY vehicle_id) a "
                    + "JOIN avl_reports b ON a.vehicle_id=b.vehicle_id AND a.maxTime = b.time "
                    + "JOIN vehicle_configs vC ON a.vehicle_id=vC.id";
        }
        if (agency.getDbType().equals("postgresql")) {
            sql = "select a.vehicle_id as \"vehicleId\", vC.name as \"name\", a.maxTime as"
                    + " \"maxTime\", lat, lon from ( SELECT vehicle_id, max(time) AS maxTime"
                    + " FROM avl_reports WHERE time > now() + '-24 hours' GROUP BY vehicle_id) a"
                    + " JOIN avl_reports b ON a.vehicle_id=b.vehicle_id AND a.maxTime = b.time"
                    + " JOIN vehicle_configs vC ON a.vehicle_id=vC.id";
        }

        String json = null;
        json = GenericJsonQuery.getJsonString(agencyId, sql);
        return json;
    }

    public static boolean hasLastAvlJsonInHours(String agencyId, String vehicleId, int hours) {
        WebAgency agency = WebAgency.getCachedWebAgency(agencyId);
        String sql = "";
        if (agency.getDbType().equals("postgresql")) {
            // sql query should be better
            sql = "select a.vehicle_id as \"vehicleId\", vC.name as \"name\", a.maxTime as"
                    + " \"maxTime\", lat, lon from ( SELECT vehicle_id, max(time) AS maxTime"
                    + " FROM avl_reports WHERE time > now() + '-"
                    + hours + " hours' AND vehicle_id = '" + vehicleId
                    + "' "
                    + "GROUP BY vehicle_id) a "
                    + "JOIN avl_reports b ON a.vehicle_id=b.vehicle_id AND a.maxTime = b.time "
                    + "JOIN vehicle_configs vC ON a.vehicle_id=vC.id";
        }

        String json = GenericJsonQuery.getJsonString(agencyId, sql);
        return json.length() > 50;
    }
}
