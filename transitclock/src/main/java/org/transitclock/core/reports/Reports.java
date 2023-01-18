package org.transitclock.core.reports;

import java.text.ParseException;

import org.transitclock.db.webstructs.WebAgency;
import org.transitclock.utils.Time;

public class Reports {
	
private static final int MAX_ROWS = 50000;
	
	/**
	 * Queries agency for AVL data and returns result as a JSON string. Limited
	 * to returning MAX_ROWS (50,000) data points.
	 * 
	 * @param agencyId
	 * @param vehicleId
	 *            Which vehicle to get data for. Set to null or empty string to
	 *            get data for all vehicles
	 * @param beginDate
	 *            date to start query
	 * @param numdays
	 *            of days to collect data for
	 * @param beginTime
	 *            optional time of day during the date range
	 * @param endTime
	 *            optional time of day during the date range
	 * @return AVL reports in JSON format. Can be empty JSON array if no data
	 *         meets criteria.
	 */
	public static String getAvlJson(String agencyId, String vehicleId,
			String beginDate, String numdays, String beginTime, String endTime) {
		//Determine the time portion of the SQL
		String timeSql = "";
		WebAgency agency = WebAgency.getCachedWebAgency(agencyId);
		// If beginTime or endTime set but not both then use default values
		if ((beginTime != null && !beginTime.isEmpty())
				|| (endTime != null && !endTime.isEmpty())) {
			if (beginTime == null || beginTime.isEmpty())
				beginTime = "00:00";
			if (endTime == null || endTime.isEmpty())
				endTime = "24:00";
		}
		//cast('2000-01-01 01:12:00'::timestamp as time);
		if (beginTime != null && !beginTime.isEmpty() 
				&& endTime != null && !endTime.isEmpty()) {
			if ("mysql".equals(agency.getDbType())) {
				timeSql = " AND time(time) BETWEEN '" 
						+ beginTime + "' AND '" + endTime + "' ";
			} else {
				timeSql = " AND cast(time::timestamp as time) BETWEEN '" 
						+ beginTime + "' AND '" + endTime + "' ";
			}
				
		}
		
		String sql = "";		
		
		
		if ("mysql".equals(agency.getDbType())) {
			sql = "SELECT vehicleId, name, time, assignmentId, lat, lon, speed, "
				+ "heading, timeProcessed, source "
				+ "FROM avlreports "
				+ "INNER JOIN vehicleconfigs ON vehicleconfigs.id = avlreports.vehicleId "
				+ "WHERE time BETWEEN " + " cast(? as datetime)"
				+ " AND " + "date_add(cast(? as datetime), INTERVAL " + numdays + " day) "
				+ timeSql;
		} else {
			sql = "SELECT vehicleId, name, time, assignmentId, lat, lon, speed, "
				+ "heading, timeProcessed, source "
				+ "FROM avlreports "
				+ "INNER JOIN vehicleconfigs ON vehicleconfigs.id = avlreports.vehicleId "
				+ "WHERE time BETWEEN " + " cast(? as timestamp)"
				+ " AND " + "cast(? as timestamp)"  + " + INTERVAL '" + numdays + " day' "
				+ timeSql;
		}

		// If only want data for single vehicle then specify so in SQL
		if (vehicleId != null && !vehicleId.isEmpty())
			sql += " AND vehicleId='" + vehicleId + "' ";
		
		// Make sure data is ordered by vehicleId so that can draw lines 
		// connecting the AVL reports per vehicle properly. Also then need
		// to order by time to make sure they are in proper order. And
		// lastly, limit AVL reports to 5000 so that someone doesn't try
		// to view too much data at once.

		sql += "ORDER BY vehicleId, time LIMIT " + MAX_ROWS;
		
		String json=null;
		try {
			java.util.Date startdate = Time.parseDate(beginDate);						
			
			json = GenericJsonQuery.getJsonString(agencyId, sql,startdate, startdate);
				
		} catch (ParseException e) {			
			json=e.getMessage();
		}						

		return json;
	}
}