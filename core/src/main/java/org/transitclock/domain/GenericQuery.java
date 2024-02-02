/* (C)2023 */
package org.transitclock.domain;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import org.transitclock.domain.webstructs.WebAgency;
import org.transitclock.utils.IntervalTimer;
import org.transitclock.utils.Time;

/**
 * For doing a query without using Hibernate. By using regular JDBC and avoiding Hibernate can
 * connect to multiple databases of different types.
 *
 * @author SkiBu Smith
 */
@Slf4j
public class GenericQuery {

    // Number of rows read in
    private int rows;

    // For caching db connection
    private static Connection connection;

    /**
     * Constructor
     *
     * @param dbType
     * @param dbHost
     * @param dbName
     * @param dbUserName
     * @param dbPassword
     * @throws SQLException
     */
    public GenericQuery(String dbType, String dbHost, String dbName, String dbUserName, String dbPassword)
            throws SQLException {
        connection = getConnection(dbType, dbHost, dbName, dbUserName, dbPassword);
    }

    /**
     * Constructor
     *
     * @param agencyId
     * @throws SQLException
     */
    public GenericQuery(String agencyId) throws SQLException {
        // Get the web agency. If it is really old, older than an hour then
        // update the cache in case the db was moved.
        WebAgency agency = WebAgency.getCachedWebAgency(agencyId, Time.HOUR_IN_MSECS);
        connection = getConnection(
                agency.getDbType(),
                agency.getDbHost(),
                agency.getDbName(),
                agency.getDbUserName(),
                agency.getDbPassword());
    }

    /**
     * Gets a database connection to be used for the query
     *
     * @param dbType
     * @param dbHost
     * @param dbName
     * @param dbUserName
     * @param dbPassword
     * @return
     * @throws SQLException
     */
    public static Connection getConnection(
            String dbType, String dbHost, String dbName, String dbUserName, String dbPassword) throws SQLException {
        Properties connectionProps = new Properties();
        connectionProps.put("user", dbUserName);
        connectionProps.put("password", dbPassword);

        // GenericQuery will likely be used by a web server. A web server
        // uses Hibernate to load web server related data and Hibernate
        // will be configured for the type of db being used (postGres or mySQL).
        // But when doing a query on an agency might be using any kind of
        // database. To get a connection the proper driver needs to first be
        // loaded. If the database for the agency happens to be different than
        // that used for the web server then need to load in the driver for
        // the agency database manually by using Class.forName().
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("Could not load in db driver for GenericQuery. {}", e.getMessage());
        }

        String url = "jdbc:" + dbType + "://" + dbHost + "/" + dbName;
        return DriverManager.getConnection(url, connectionProps);
    }

    /**
     * Performs the specified generic query. A List of GenericResult objects is returned. All number
     * columns (integer or float) are placed in GenericResult.numbers. A string column is assumed to
     * be a tooltip and is put in GenericResult.text.
     *
     * @param sql
     * @return List of GenericResult. If no data then returns empty list (instead of null)
     * @throws SQLException
     */
    protected void doQuery(String sql, Object... parameters) throws SQLException {

        IntervalTimer timer = new IntervalTimer();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // TODO Deal with dates for the moment
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] instanceof java.util.Date) {
                    statement.setTimestamp(i + 1, new Timestamp(((java.util.Date) parameters[i]).getTime()));
                }
            }

            ResultSet rs = statement.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();

            // Add all the columns by calling subclass addColumn()
            for (int i = 1; i <= metaData.getColumnCount(); ++i) {
                addColumn(metaData.getColumnLabel(i), metaData.getColumnType(i));
            }
            doneWithColumns();

            // Process each row of data
            rows = 0;
            while (rs.next()) {
                ++rows;

                List<Object> row = new ArrayList<Object>();
                for (int i = 1; i <= metaData.getColumnCount(); ++i) {
                    row.add(rs.getObject(i));
                }
                addRow(row);
            }

            rs.close();

            logger.debug("GenericQuery query took {}msec rows={}", timer.elapsedMsec(), rows);
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Executes an INSERT, UPDATE, or DELETE statement.
     *
     * @param sql The SQL to be executed
     * @throws SQLException
     */
    public void doUpdate(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Returns number of rows read in.
     *
     * @return
     */
    protected int getNumberOfRows() {
        return rows;
    }

    /**
     * Called for each column when processing query data
     *
     * @param columnName
     * @param type java.sql.Types such as Types.DOUBLE
     */
    protected void addColumn(String columnName, int type) {}

    /**
     * When done processing columns. Allows subclass to insert separator between column definitions
     * and the row data
     */
    protected void doneWithColumns() {}

    /**
     * Called for each row when processing query data.
     *
     * @param values The values for the row.
     */
    protected void addRow(List<Object> values) {}
}
