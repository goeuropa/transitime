<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="org.transitime.reports.GenericJsonQuery" %>
<%@ page import="org.transitime.reports.SqlUtils" %>
<%
String sql =
	"SELECT COUNT(CASE WHEN time-scheduledtime > '00:02:00' THEN 1 ELSE null END) AS late, \n" 
	+ "     COUNT(CASE WHEN scheduledtime-time > '00:01:00' THEN 1 ELSE null END) as early, \n"
    + "     COUNT(*) AS total, \n"
    + "     100.0 * COUNT(CASE WHEN time-scheduledtime > '00:02:00' THEN 1 ELSE null END)/count(*) AS late_percent, \n"
    + "     r.name \n"
    + "FROM arrivalsdepartures ad, routes r \n"
    + "WHERE "
    // For joining in route table to get route name
    + "ad.configrev = r.configrev \n"
    + " AND ad.routeshortname = r.shortname \n"
    // Only need arrivals/departures that have a schedule time
    + " AND ad.scheduledtime IS NOT NULL \n"
    // Specifies which routes to provide data for
    + SqlUtils.routeClause(request, "ad") + "\n"
    + SqlUtils.timeRangeClause(request, "ad.time", 31) + "\n"
    // Grouping needed since want to output route name
    + " GROUP BY r.name;";

// Just for debugging
System.out.println("\nFor schedule adherence query sql=\n" + sql);
    		
// Do the query and return result in JSON format    
String agencyId = request.getParameter("a");
String jsonString = GenericJsonQuery.getJsonString(agencyId, sql);
response.setContentType("application/json");
response.getWriter().write(jsonString);
%>