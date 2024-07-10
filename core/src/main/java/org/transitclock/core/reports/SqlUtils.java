/* (C)2023 */
package org.transitclock.core.reports;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.transitclock.utils.Time;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * SQL utilities for creating SQL statements using parameters passed in to a page. Intended to make
 * creating sql for route and time based queries easier to create.
 *
 * @author Michael Smith
 */
@Slf4j
public class SqlUtils {

    /**
     * To be called on request parameters to make sure they don't contain any SQL injection
     * trickery.
     *
     * @param parameter
     * @throws RuntimeException if problem characters detected
     */
    public static void throwOnSqlInjection(String parameter) {
        // If null then it is not a problem
        if (parameter == null) {
            return;
        }

        // If parameter contains a ' or a ; then throw error to
        // prevent possible SQL injection attack
        if (parameter.contains("'") || parameter.contains(";")) {
            throw new IllegalArgumentException("Parameter \"" + parameter + "\" not valid.");
        }
    }


    /**
     * Returns SQL list of route identifiers specified by the request parameter "r". Multiple routes
     * can be specified. These route identifiers might be route_ids or route_short_names.
     *
     * @param r route
     * @return an sql list such as "('21','5','5R')"
     */
    public static String routeIdentifiersList(String r) {
        String[] routeIdentifiers = {r};
        StringBuilder sb = new StringBuilder();
        sb.append(" (");
        boolean needComma = false;
        for (String routeIdentifier : routeIdentifiers) {
            throwOnSqlInjection(routeIdentifier);

            if (needComma) {
                sb.append(',');
            }
            needComma = true;

            sb.append('\'').append(routeIdentifier).append('\'');
        }
        sb.append(") ");

        return sb.toString();
    }

    /**
     * Creates a SQL clause for specifying routes. Looks at the request parameter "r". It can be
     * empty, signifying all routes, or can specify multiple routes. If "r" parameter not specified
     * in request then empty string is returned such that data for all routes will be retrieved.
     * Since "AND" is included in clause if it is not empty string this clause can't be put right
     * after the WHERE clause
     *
     * @param r route no
     * @param tableAliasName for when joins are used such that a table has an alias such as "FROM
     *     arrivalsdepartures ad, routes r"
     * @return SQL clause such as "AND ad.routeshortname = r.shortname AND ad.scheduledtime IS NOT
     *     NULL AND (ad.routeshortname IN ('21','5') OR ad.routeid IN ('21','5') )"
     */
    public static String routeClause(String r, String tableAliasName) {
        if (r == null || r.isEmpty())
            return "";

        String routeIdentifiers = routeIdentifiersList(r);

        // For specifying table alias name for when doing more
        // complicated queries
        String tableAlias = "";
        if (tableAliasName != null && !tableAliasName.isEmpty()) tableAlias = tableAliasName + ".";

        return " AND " + tableAlias + "route_short_name IN " + routeIdentifiers;
    }

    public static String stopClause(String id, String tableAliasName) {
        if (id == null || id.isEmpty())
            return "";

        String tableAlias = "";
        if (tableAliasName != null && !tableAliasName.isEmpty()) tableAlias = tableAliasName + ".";

        return " AND " + tableAlias + "stop_id = '" + id + "'";
    }

    /**
     * Creates a SQL clause for specifying a time range. Looks at the request parameters
     * "beginDate", "numDays", "beginTime", and "endTime"
     *
     * @param request Http request containing parameters for the query
     * @param timeColumnName name of time column for that for query
     * @param maxNumDays maximum number of days for query. Request parameter numDays is limited to
     *     this value in order to make sure that query doesn't try to process too much data.
     * @return SQL string such as "AND ad.time BETWEEN '10/30/2015' AND TIMESTAMP '10/30/2015' +
     *     INTERVAL '1 day' AND time::time BETWEEN '12:00' AND '24:00'"
     */
    public static String timeRangeClause(HttpServletRequest request, String timeColumnName, int maxNumDays) {
        String beginTime = request.getParameter("beginTime");
        throwOnSqlInjection(beginTime);

        String endTime = request.getParameter("endTime");
        throwOnSqlInjection(endTime);

        // Determine the time portion of the SQL
        // If beginTime or endTime set but not both then use default values
        if (beginTime == null || beginTime.isEmpty()) {
            beginTime = "00:00";
        }
        if (endTime == null || endTime.isEmpty()) {
            endTime = "24:00";
        }
        String timeSql = " AND " + timeColumnName + "::time BETWEEN '" + beginTime + "' AND '" + endTime + "' ";

        String dateRange = request.getParameter("dateRange");
        throwOnSqlInjection(dateRange);
        if (dateRange != null) {
            String[] fromToDates = dateRange.split(" to ");
            String beginDateStr, endDateStr;
            if (fromToDates.length == 1) {
                beginDateStr = endDateStr = fromToDates[0];
            } else {
                beginDateStr = fromToDates[0];
                endDateStr = fromToDates[1];
            }

            // Make sure not running report for too many days
            try {
                long beginDateTime = Time.parseDate(beginDateStr).getTime();
                long endDateTime = Time.parseDate(endDateStr).getTime();
                if (endDateTime - beginDateTime >= maxNumDays * Time.DAY_IN_MSECS) {
                    throw new IllegalArgumentException("Date range is limited to " + maxNumDays + " days.");
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException(
                        "Could not parse begin date \"" + beginDateStr + "\" or end date \"" + endDateStr + "\".");
            }

            return " AND %s BETWEEN '%s'  AND TIMESTAMP '%s' + INTERVAL '1 day' %s "
                    .formatted(timeColumnName, beginDateStr, endDateStr, timeSql);
        } else { // Not using dateRange so must be using beginDate and numDays params
            String beginDate = request.getParameter("beginDate");
            throwOnSqlInjection(beginDate);

            String numDaysStr = request.getParameter("numDays");
            throwOnSqlInjection(numDaysStr);

            if (numDaysStr == null) {
                numDaysStr = "1";
            }
            // Limit number of days to maxNumDays to prevent queries that are
            // too big
            int numDays = Integer.parseInt(numDaysStr);


            if (numDays > maxNumDays) {
                numDays = maxNumDays;
            }

            SimpleDateFormat currentFormat = new SimpleDateFormat("MM-dd-yyyy");
            SimpleDateFormat requiredFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                beginDate = requiredFormat.format(currentFormat.parse(beginDate));
            } catch (ParseException e) {
                logger.error("Exception occurred while processing time-range clause.", e);
            }
            return " AND %s BETWEEN '%s'  AND TIMESTAMP '%s' + INTERVAL '%d day' %s "
                    .formatted(timeColumnName, beginDate, beginDate, numDays, timeSql);
        }
    }

    /**
     * Creates a SQL clause for specifying a time range. Looks at the request parameters
     * "beginDate", "numDays", "beginTime", and "endTime"
     *
     * @param request Http request containing parameters for the query
     * @param timeColumnName name of time column for that for query
     * @param maxNumDays maximum number of days for query. Request parameter numDays is limited to
     *     this value in order to make sure that query doesn't try to process too much data.
     * @return SQL string such as "AND ad.time BETWEEN '10/30/2015' AND TIMESTAMP '10/30/2015' +
     *     INTERVAL '1 day' AND time::time BETWEEN '12:00' AND '24:00'"
     */
    public static String timeRangeClause(
            String timeColumnName,
            int maxNumDays,
            int numDays,
            String beginTime,
            String endTime,
            String beginDate) {
        throwOnSqlInjection(beginTime);
        throwOnSqlInjection(endTime);

        // If beginTime or endTime set but not both then use default values
        if (beginTime == null || beginTime.isEmpty()) {
            beginTime = "00:00";
        }
        if (endTime == null || endTime.isEmpty()) {
            endTime = "24:00";
        }

        String timeSql = " AND " + timeColumnName + "::time BETWEEN '" + beginTime + "' AND '" + endTime + "' ";

        throwOnSqlInjection(beginDate);

        if (numDays > maxNumDays) {
            numDays = maxNumDays;
        }

        SimpleDateFormat currentFormat = new SimpleDateFormat("MM-dd-yyyy");
        SimpleDateFormat requiredFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
        if (beginDate.charAt(4) != '-') {
            beginDate = requiredFormat.format(currentFormat.parse(beginDate));
        } else {
            requiredFormat.parse(beginDate);
        }
        } catch (ParseException e) {
            logger.error("Exception happened while processing time-range clause", e);
        }

        return " AND %s BETWEEN '%s' AND TIMESTAMP '%s' + INTERVAL '%d day' %s "
                .formatted(timeColumnName, beginDate, beginDate, numDays, timeSql);
    }

    /**
     * Converts minutes string to seconds.
     *
     * @param minutes
     * @return seconds
     */
    public static int convertMinutesToSecs(String minutes) {
        return (int) Double.parseDouble(minutes) * Time.SEC_PER_MIN;
    }
}
