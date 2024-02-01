# :fire::fire: TransitTime :fire::fire:

The complete core Java software for the Transitime real-time transit information project. The goal is to use any type of real-time GPS data to generate useful public transportation information. The system is for both letting passengers know the status of their vehicles and helping agencies more effectively manage their systems. By providing a complete open-source system, agencies can have a cost effective system and have full ownership of it. 

The software is currently being used in a production environment for MBTA Commuter Rail and for several smaller agencies.

## :hammer: Setup
In order to build & run the application make sure you have the following requirements fulfilled:
- [ ] you have maven installed
- [ ] you have java 17 or later installed
- [ ] you have docker installed

### :wrench: Building
The software is made up of multiple modules which can each be built with maven.
The core functionality is in the [core](core) project. The REST api & webapp is in [app](app) folder.

```shell
# this will build all the modules of the application
mvn clean install
```

### :runner: Running
In order to run the application you need a postgres database available, or you could use docker to start an instance
```shell
docker run --rm -e POSTGRES_PASSWORD=transitclock -e POSTGRES_DB=transitclock -p 5432:5432 postgres:15-alpine
```

#### :blue_book: Configuration
- **Properties file**

After docker has started, and you have postgress running you need to fine tune the application properties
Please adjust the fields were you see comments starting with ```##!!!```.

```properties
##!!! STATIC GTFS FEED
transitclock.gtfs.url=
transitclock.gtfs.dirName=/var/transitclock/cache
transitclock.gtfs.intervalMsec=86400000
transitclock.autoBlockAssigner.autoAssignerEnabled=true
transitclock.autoBlockAssigner.ignoreAvlAssignments=false
transitclock.autoBlockAssigner.allowableEarlySeconds=600
transitclock.autoBlockAssigner.allowableLateSeconds=600

transitclock.avl.feedTimeoutInMSecs=30000
##!!! RT FEED URI
transitclock.avl.gtfsRealtimeFeedURI=
transitclock.avl.maxSpeed=100
transitclock.avl.numThreads=1
transitclock.avl.queueSize=2400
transitclock.avl.minLatitude=43
transitclock.avl.maxLatitude=48
transitclock.avl.maxLongitude=30
transitclock.avl.minLongitude=20
transitclock.avl.feedPollingRateSecs=15

transitclock.blockLoading.agressive=false

transitclock.cache.core.daysPopulateHistoricalCache=0

transitclock.core.agencyId=stpt
transitclock.core.allowableEarlyDepartureTimeForLoggingEvent=180
transitclock.core.allowableEarlyForLayoverSeconds=1800
transitclock.core.allowableEarlySecondsForInitialMatching=1200
transitclock.core.allowableEarlyTimeForEarlyDepartureSecs=180
transitclock.core.allowableLateAtTerminalForLoggingEvent=240
transitclock.core.allowableLateDepartureTimeForLoggingEvent=360
transitclock.core.allowableLateSeconds=2700
transitclock.core.allowableNumberOfBadMatches=4
transitclock.core.afterStopDistance=100
transitclock.core.beforeStopDistance=100
transitclock.core.cache.tripDataHistoryCache=org.transitclock.core.dataCache.ehcache.scheduled.TripDataHistoryCache
transitclock.core.cache.errorCacheClass=org.transitclock.core.dataCache.ehcache.KalmanErrorCache
transitclock.core.cache.stopArrivalDepartureCache=org.transitclock.core.dataCache.ehcache.StopArrivalDepartureCache
transitclock.core.cache.stopPathPredictionCache=org.transitclock.core.dataCache.StopPathPredictionCache
transitclock.core.cache.dwellTimeModelCache=org.transitclock.core.dataCache.ehcache.scheduled.DwellTimeModelCache
transitclock.core.distanceFromLayoverForEarlyDeparture=250
transitclock.core.dwelltime.model=org.transitclock.core.predictiongenerator.scheduled.dwell.DwellAverage
transitclock.core.dwelltime.headwayGeneratorClass=org.transitclock.core.headwaygenerator.LastArrivalsHeadwayGenerator
transitclock.core.exclusiveBlockAssignments=true
transitclock.core.fillHistoricalCaches=0
transitclock.core.layoverDistance=1000
transitclock.core.longDistanceDeadheadingSpeed=20
transitclock.core.matchHistoryMaxSize=40
transitclock.core.maxHeadingOffsetFromSegment=200
transitclock.core.maxPredictionTimeForDbSecs=1200
transitclock.core.maxPredictionsTimeSecs=1800
transitclock.core.minDistanceForDelayed=60
transitclock.core.minDistanceForNoProgress=60
transitclock.core.onlyNeedArrivalDepartures=false
#transitclock.core.predictionGeneratorClass=org.transitclock.core.predictiongenerator.scheduled.traveltime.kalman.KalmanPredictionGeneratorImpl
transitclock.core.shortDistanceDeadheadingSpeed=10
transitclock.core.timeForDeterminingDelayedSecs=300
transitclock.core.timeForDeterminingNoProgress=360000
transitclock.core.trackHistoricalCaches=false
transitclock.core.useArrivalPredictionsForNormalStops=false

transitclock.db.batchSize=4000
transitclock.db.dbName=STPT
transitclock.db.dbPassword=transitclock
transitclock.db.dbUserName=transitclock
transitclock.db.dbHost=localhost:5432
transitclock.db.storeDataInDatabase=true
transitclock.db.dbType=postgresql


transitclock.predAccuracy.stopsPerTrip=1000
transitclock.predAccuracy.maxPredTimeMinutes=30

transitclock.prediction.data.kalman.mindays=3
transitclock.prediction.data.kalman.maxdays=5
transitclock.prediction.data.kalman.maxdaystosearch=21
transitclock.prediction.data.kalman.percentagePredictionMethodDifferencene=50
transitclock.prediction.data.kalman.tresholdForDifferenceEventLog=60000
transitclock.prediction.rls.lambda=0.9

transitclock.timeout.pollingRateSecs=60

transitclock.modules.optionalModulesList=org.transitclock.core.avl.GtfsRealtimeModule;org.transitclock.core.predAccuracy.PredictionAccuracyModule;org.transitclock.gtfs.GtfsUpdatedModule
##!!! absolute location to the hibernate.cfg.xml file
transitclock.hibernate.configFile=
transitclock.logging.dir=/var/transitclock/logs
transitclock.web.mapTileUrl=http://tile.openstreetmap.org/{z}/{x}/{y}.png
```

- **hibernate config file sample**
```xml
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE hibernate-configuration SYSTEM
        "http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">

<hibernate-configuration>
    <session-factory>
        <property name="hibernate.connection.driver_class">
            org.postgresql.Driver
        </property>
        <property name="hibernate.hikari.autoCommit">false</property>

        <!-- Enable full SQL logging that also shows the
             parameters being used. show_sql is not really
             useful since it duplicates the output of the SQL.
             format_sql makes sql more readable but takes up
             more space. OK for now but should disable in the
             future. use_sql_comments adds comments that might
             be nice, but again, should probably be disabled
             in the future. -->
<!--        <property name="hibernate.show_sql">true</property>-->
<!--        <property name="hibernate.format_sql">true</property>-->
<!--        <property name="hibernate.use_sql_comments">true</property>-->
        <property name="hibernate.hbm2ddl.auto">create-drop</property>
        <property name="hibernate.hikari.minimumIdle">5</property>
        <property name="hibernate.hikari.maximumPoolSize">20</property>
        <property name="hibernate.hikari.idleTimeout">30000</property>
        <property name="hibernate.jdbc.batch_size">25</property>
 </session-factory>
</hibernate-configuration>
```

#### :bomb: Starting the application
```shell
JAVA_OPTS="-Dtransitclock.configFiles=/location-to-properties-file/transitclock.properties -Dtransitclock.apikey=f78a2e9a"
GTFS_TO_IMPORT="url to gtfs you want to import"

java $JAVA_OPTS -jar app/target/transitclock.jar --gtfs-url $GTFS_TO_IMPORT
```

### :whale: Running using Docker
Simplest way to run the transitclock would be using ```docker```, actually ```docker compose```. For it to happen you would be to map your config into the transitclock container using a configuration as follows:
```yaml
version: "3.18"
services:
  db:
    image: postgres:15-alpine
    restart: always
    environment:
      POSTGRES_PASSWORD: transitclock
      POSTGRES_DB: transitclock
    ports:
      - "5432:5432"
  gtfsrt-validator:
    image: ghcr.io/mobilitydata/gtfs-realtime-validator:latest
    ports:
      - "9090:8080"
  transitclock:
    image: otrro/transitclock-server:latest
    depends_on:
      - db
    environment:
      AGENCYID: transitclock # not that relevant anymore since will be coming from your file
      AGENCYNAME: transitclock # has to match what is configured in the file for transitclock.db.dbName and should match POSTGRES_DB specified for postgress
      GTFS_URL: https://your feed location
      GTFSRTVEHICLEPOSITIONS: https://rt feed location # can be configured in the file directly
      PGPASSWORD: transitclock
      POSTGRES_PORT_5432_TCP_ADDR: db
      POSTGRES_PORT_5432_TCP_PORT: 5432
    volumes:
      # this mapps the transitclock.properties file that is near to docker-compose file to the one in container that is used by the app
      ### IMPORTANT NOTE: make sure you have this config transitclock.hibernate.configFile=/app/config/hibernate.cfg.xml
      - ./transitclock.properties:/app/config/transitclock.properties
    ports:
      - "8080:8080"
    command:
      - --gtfs-url
      - https://your feed location
```

After doing this you just simply need to:
```shell
# if you have compose plugin for docker installed
docker compose up # in the folder where you have save the previous content as docker-compose.yaml

# if you have the actual docker-compose application
docker-compose up
```