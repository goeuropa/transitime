/* (C)2023 */
package org.transitclock.domain.hibernate;

import lombok.Synchronized;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.NativeQuery;
import org.hibernate.service.ServiceRegistry;
import org.transitclock.config.data.DbSetupConfig;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class HibernateUtils {

    // Cache. Keyed on database name
    private static final Map<String, SessionFactory> sessionFactoryCache = new ConcurrentHashMap<>();
    private static final Map<Thread, ThreadLocal<Session>> threadSessions = new ConcurrentHashMap<>();

    private static SessionFactory createSessionFactory(String dbName, boolean readOnly) throws HibernateException {
        Configuration config = new Configuration();

        // Want to be able to specify a configuration file for now
        // since developing in Eclipse and want all config files
        // to be in same place. But the Config.configure(String)
        // method can't seem to work with a Windows directory name such
        // as C:/users/Mike/software/hibernate.cfg.xml . Therefore create
        // a File object for that file name and pass in the File object
        // to configure().
        String fileName = DbSetupConfig.getHibernateConfigFileName();
        logger.info("Configuring Hibernate for dbName={} using config file={}", dbName, fileName);
        File f = new File(fileName);
        if (!f.exists()) {
            logger.info("The Hibernate file {} doesn't exist as a regular file so seeing if it is in classpath.", fileName);

            // Couldn't find file directly so look in classpath for it
            ClassLoader classLoader = HibernateUtils.class.getClassLoader();
            URL url = classLoader.getResource(fileName);
            if (url != null) {
                f = new File(url.getFile());
            }
        }

        if (f.exists()) {
            config.configure(f);
        } else {
            logger.error("Could not load in hibernate config file {}", fileName);
        }

        // Add the annotated classes so that they can be used
        AnnotatedClassesList.addAnnotatedClasses(config);

        // Set the db info for the URL, username, and password. Uses the
        // property hibernate.connection.url if it is set so that everything
        // can be overwritten in a standard way. If that property not set then
        // uses values from DbSetupConfig if set. If they are not set then the
        // values will be obtained from the hibernate.cfg.xml config file.
        String dbUrl = config.getProperty(AvailableSettings.URL);
        if (readOnly) {
            dbUrl = config.getProperty("hibernate.ro.connection.url");
            // override the configured url so its picked up by the driver
            config.setProperty(AvailableSettings.URL, dbUrl);
            logger.trace("using read only connection url {}", dbUrl);
        }
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "jdbc:" + DbSetupConfig.getDbType() + "://" + DbSetupConfig.getDbHost() + "/" + dbName;

            // If socket timeout specified then add that to the URL
            Integer timeout = DbSetupConfig.getSocketTimeoutSec();
            if (timeout != null && timeout != 0) {
                // If mysql then timeout specified in msec instead of secs
                if (DbSetupConfig.getDbType().equals("mysql")) {
                    timeout *= 1000;
                }

                dbUrl += "?connectTimeout=" + timeout + "&socketTimeout=" + timeout;
            }
            config.setProperty(AvailableSettings.URL, dbUrl);
        }

        String dbUserName = DbSetupConfig.getDbUserName();
        if (dbUserName != null) {
            config.setProperty(AvailableSettings.USER, dbUserName);
        } else {
            dbUserName = config.getProperty(AvailableSettings.USER);
        }

        if (DbSetupConfig.getDbPassword() != null) {
            config.setProperty(AvailableSettings.PASS, DbSetupConfig.getDbPassword());
        }

        // Log info, but don't log password. This can just be debug logging
        // even though it is important because the C3P0 connector logs the info.
        logger.info("For Hibernate factory project dbName={} using url={} username={}, and configured password",
                dbName,
                dbUrl,
                dbUserName);

        // Get the session factory for persistence
        Properties properties = config.getProperties();
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(properties).build();

        // Return the factory
        return config.buildSessionFactory(serviceRegistry);
    }

    /**
     * Returns a cached Hibernate SessionFactory. Returns null if there is a problem.
     *
     * @param agencyId Used as the database name if the property transitclock.db.dbName is not set
     * @return {@link SessionFactory}
     */
    public static SessionFactory getSessionFactory(String agencyId) throws HibernateException {
        return getSessionFactory(agencyId, false);
    }

    private static SessionFactory getSessionFactory(String agencyId, boolean readOnly) throws HibernateException {
        // Determine the database name to use. Will usually use the
        // projectId since each project has a database. But this might
        // be overridden by the transitclock.core.dbName property.
        String dbName = DbSetupConfig.getDbName();
        if (dbName == null) dbName = agencyId;

        if (readOnly) {
            dbName = dbName + "-ro";
        }

        SessionFactory factory;

        synchronized (sessionFactoryCache) {
            factory = sessionFactoryCache.get(dbName);
            // If factory not yet created for this projectId then create it
            if (factory == null || factory.isClosed()) {
                try {
                    factory = createSessionFactory(dbName, readOnly);
                    sessionFactoryCache.put(dbName, factory);
                } catch (Exception e) {
                    logger.error("Could not create SessionFactory for " + "dbName={}", dbName, e);
                    throw e;
                }
            }
        }

        return factory;
    }

    /**
     * Clears out the session factory so that a new one will be created for the dbName. This way new
     * db connections are made. This is useful for dealing with timezones and postgres. For that
     * situation want to be able to read in timezone from db so can set default timezone. Problem
     * with postgres is that once a factory is used to generate sessions the database will continue
     * to use the default timezone that was configured at that time. This means that future calls to
     * the db will use the wrong timezone! Through this function one can read in timezone from
     * database, set the default timezone, clear the factory so that future db connections will use
     * the newly configured timezone, and then successfully process dates.
     */
    public static void clearSessionFactory() {
        sessionFactoryCache.forEach((s, sessionFactory) -> {
            if(sessionFactory.isOpen()) {
                sessionFactory.close();
            }
        });
        sessionFactoryCache.clear();
    }

    /**
     * Returns session for the specified agencyId.
     *
     * <p>NOTE: Make sure you close the session after the query!! Use a try/catch around the query
     * and close the session in a finally block to make sure it happens. The system only gets a
     * limited number of sessions!!
     *
     * @param agencyId Used as the database name if the property transitclock.core.dbName is not set
     * @return The Session. Make sure you close it when done because system only gets limited number
     *     of open sessions.
     * @throws HibernateException
     */
    public static Session getSession(String agencyId) throws HibernateException {
        return getSession(agencyId, false);
    }

    public static Session getSession(String agencyId, boolean readOnly) throws HibernateException {
        SessionFactory sessionFactory = HibernateUtils.getSessionFactory(agencyId, readOnly);
        return sessionFactory.openSession();
    }

    /**
     * Returns the session for the database name specified by the transitclock.db.dbName Java
     * property.
     *
     * <p>NOTE: Make sure you close the session after the query!! Use a try/catch around the query
     * and close the session in a finally block to make sure it happens. The system only gets a
     * limited number of sessions!!
     *
     * @return The Session. Make sure you close it when done because system only gets limited number
     *     of open sessions.
     */
    public static Session getSession() {
        return getSession(false);
    }


    @Synchronized
    public static Session getSession(boolean readOnly) {
        return threadSessions
                .compute(Thread.currentThread(), (t, s) -> {
                    String dbName = DbSetupConfig.getDbName();
                    if (s == null) {
                        ThreadLocal<Session> sessionThreadLocal = new ThreadLocal<>();
                        sessionThreadLocal.set(ContextAwareSession.create(getSessionFactory(dbName, readOnly)));
                        return sessionThreadLocal;
                    }

                    Session session = s.get();
                    if (!session.isOpen() || session.isDefaultReadOnly() != readOnly) {
                        s.set(ContextAwareSession.create(getSessionFactory(dbName, readOnly)));
                    }

                    return s;
                })
                .get();
    }


    private static class ContextAwareSession implements Session {
        @Delegate
        private final Session session;
        private final Thread thread;

        public static ContextAwareSession create(SessionFactory sessionFactory) {
            return new ContextAwareSession(sessionFactory.openSession(), Thread.currentThread());
        }

        public ContextAwareSession(Session session, Thread thread) {
            this.session = session;
            this.thread = thread;
        }

        public Thread getContext() {
            return thread;
        }

        @Override
        public void close() throws HibernateException {
            session.close();
            threadSessions.remove(thread);
        }

        @Override
        public NativeQuery createNativeQuery(String sqlString, Class resultClass) {
            return session.createNativeQuery(sqlString, resultClass);
        }

        @Override
        public ProcedureCall createStoredProcedureQuery(String procedureName, Class... resultClasses) {
            return session.createStoredProcedureQuery(procedureName, resultClasses);
        }
    }
}
