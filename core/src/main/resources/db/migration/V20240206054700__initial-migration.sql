
CREATE SEQUENCE IF NOT EXISTS active_revisions_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS export_table_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS headways_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS holding_times_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS prediction_accuracy_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS predictions_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS stop_path_predictions_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS travel_times_for_stop_paths_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS travel_times_for_trips_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS vehicle_to_block_configs_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS stoppath_locations
(
    stoppath_stop_path_id    VARCHAR(120) NOT NULL,
    stoppath_trip_pattern_id VARCHAR(120) NOT NULL,
    stoppath_config_rev      INTEGER      NOT NULL,
    lat                      DOUBLE PRECISION,
    lon                      DOUBLE PRECISION,
    list_index               INTEGER      NOT NULL,
    CONSTRAINT pk_stoppath_locations PRIMARY KEY (stoppath_trip_pattern_id, stoppath_stop_path_id, stoppath_config_rev, list_index)
);

CREATE TABLE IF NOT EXISTS trip_scheduled_times_list
(
    trip_config_rev          INTEGER     NOT NULL,
    trip_trip_id             VARCHAR(60) NOT NULL,
    trip_start_time          INTEGER     NOT NULL,
    arrival_time             INTEGER,
    departure_time           INTEGER,
    list_index               INTEGER     NOT NULL,
    CONSTRAINT pk_trip_scheduledtimeslist PRIMARY KEY (trip_config_rev, trip_trip_id, trip_start_time, list_index)
);

CREATE TABLE IF NOT EXISTS active_revisions
(
    id               INTEGER NOT NULL,
    config_rev       INTEGER,
    travel_times_rev INTEGER,
    CONSTRAINT pk_active_revisions PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS agencies
(
    config_rev      INTEGER     NOT NULL,
    agency_name     VARCHAR(60) NOT NULL,
    agency_id       VARCHAR(60),
    agency_url      VARCHAR,
    agency_timezone VARCHAR(40),
    agency_lang     VARCHAR(15),
    agency_phone    VARCHAR(15),
    agency_fare_url VARCHAR,
    min_lat         DOUBLE PRECISION,
    max_lat         DOUBLE PRECISION,
    min_lon         DOUBLE PRECISION,
    max_lon         DOUBLE PRECISION,
    CONSTRAINT pk_agencies PRIMARY KEY (config_rev, agency_name)
);

CREATE TABLE IF NOT EXISTS api_keys
(
    applicationName VARCHAR(80) NOT NULL,
    applicationKey  VARCHAR(20),
    applicationUrl  VARCHAR(80),
    email           VARCHAR(80),
    phone           VARCHAR(80),
    description     VARCHAR(1000),
    CONSTRAINT pk_api_keys PRIMARY KEY (applicationName)
);

CREATE TABLE IF NOT EXISTS arrivals_departures
(
    vehicle_id       VARCHAR(60)                 NOT NULL,
    type             VARCHAR(255),
    time             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    stop_id          VARCHAR(60)                 NOT NULL,
    gtfs_stop_seq    INTEGER                     NOT NULL,
    is_arrival       BOOLEAN                     NOT NULL,
    trip_id          VARCHAR(60)                 NOT NULL,
    config_rev       INTEGER,
    avl_time         TIMESTAMP WITHOUT TIME ZONE,
    scheduled_time   TIMESTAMP WITHOUT TIME ZONE,
    block_id         VARCHAR(60),
    route_id         VARCHAR(60),
    route_short_name VARCHAR(60),
    service_id       VARCHAR(60),
    direction_id     VARCHAR(60),
    trip_index       INTEGER,
    freq_start_time  TIMESTAMP WITHOUT TIME ZONE,
    stop_path_index  INTEGER,
    stop_order       INTEGER,
    stop_path_length FLOAT,
    CONSTRAINT pk_arrivals_departures PRIMARY KEY (vehicle_id, time, stop_id, gtfs_stop_seq, is_arrival, trip_id)
);

CREATE TABLE IF NOT EXISTS avl_reports
(
    vehicle_id         VARCHAR(60)                 NOT NULL,
    time               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    time_processed     TIMESTAMP WITHOUT TIME ZONE,
    speed              FLOAT,
    heading            FLOAT,
    source             VARCHAR(10),
    assignment_id      VARCHAR(60),
    assignment_type    VARCHAR(40),
    driver_id          VARCHAR(60),
    license_place      VARCHAR(10),
    passenger_count    INTEGER,
    passenger_fullness FLOAT,
    field1_name        VARCHAR(60),
    field1_value       VARCHAR(60),
    vehicle_name       VARCHAR(255),
    lat                DOUBLE PRECISION,
    lon                DOUBLE PRECISION,
    CONSTRAINT pk_avl_reports PRIMARY KEY (vehicle_id, time)
);

CREATE TABLE IF NOT EXISTS block_to_trip
(
    block_block_id   VARCHAR(60) NOT NULL,
    block_config_rev INTEGER     NOT NULL,
    block_service_id VARCHAR(60) NOT NULL,

    trips_trip_id    VARCHAR(60) NOT NULL,
    trips_config_rev INTEGER     NOT NULL,
    trips_start_time INTEGER     NOT NULL,

    list_index       INTEGER     NOT NULL,
    CONSTRAINT pk_block_to_trip PRIMARY KEY (block_block_id, block_config_rev, block_service_id, list_index)
);

CREATE TABLE IF NOT EXISTS blocks
(
    service_id VARCHAR(60) NOT NULL,
    config_rev INTEGER     NOT NULL,
    block_id   VARCHAR(60) NOT NULL,
    start_time INTEGER,
    end_time   INTEGER,
    route_ids  JSON,
    CONSTRAINT pk_blocks PRIMARY KEY (config_rev, block_id, service_id)
);

CREATE TABLE IF NOT EXISTS calendar_dates
(
    config_rev     INTEGER                     NOT NULL,
    service_id     VARCHAR(60)                 NOT NULL,
    date           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    exception_type VARCHAR(2),
    CONSTRAINT pk_calendar_dates PRIMARY KEY (config_rev, service_id, date)
);

CREATE TABLE IF NOT EXISTS calendars
(
    config_rev INTEGER                     NOT NULL,
    service_id VARCHAR(60)                 NOT NULL,
    monday     BOOLEAN                     NOT NULL,
    tuesday    BOOLEAN                     NOT NULL,
    wednesday  BOOLEAN                     NOT NULL,
    thursday   BOOLEAN                     NOT NULL,
    friday     BOOLEAN                     NOT NULL,
    saturday   BOOLEAN                     NOT NULL,
    sunday     BOOLEAN                     NOT NULL,
    start_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_date   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_calendars PRIMARY KEY (config_rev, service_id, monday, tuesday, wednesday, thursday, friday, saturday,
                                         sunday, start_date, end_date)
);

CREATE TABLE IF NOT EXISTS config_revisions
(
    config_rev                 INTEGER NOT NULL,
    processed_time             TIMESTAMP WITHOUT TIME ZONE,
    zipfile_last_modified_time TIMESTAMP WITHOUT TIME ZONE,
    notes                      VARCHAR(512),
    CONSTRAINT pk_config_revisions PRIMARY KEY (config_rev)
);

CREATE TABLE IF NOT EXISTS export_table
(
    id            BIGINT NOT NULL,
    data_date     date,
    export_date   TIMESTAMP WITHOUT TIME ZONE,
    export_type   INTEGER,
    export_status INTEGER,
    first_name    VARCHAR(255),
    file          BYTEA,
    CONSTRAINT pk_export_table PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS fare_attributes
(
    config_rev        INTEGER     NOT NULL,
    fare_id           VARCHAR(60) NOT NULL,
    price             FLOAT,
    currency_type     VARCHAR(3),
    payment_method    VARCHAR,
    transfers         VARCHAR,
    transfer_duration INTEGER,
    CONSTRAINT pk_fare_attributes PRIMARY KEY (config_rev, fare_id)
);

CREATE TABLE IF NOT EXISTS fare_rules
(
    config_rev     INTEGER     NOT NULL,
    fare_id        VARCHAR(60) NOT NULL,
    route_id       VARCHAR(60) NOT NULL,
    origin_id      VARCHAR(60) NOT NULL,
    destination_id VARCHAR(60) NOT NULL,
    contains_id    VARCHAR(60) NOT NULL,
    CONSTRAINT pk_fare_rules PRIMARY KEY (config_rev, fare_id, route_id, origin_id, destination_id, contains_id)
);

CREATE TABLE IF NOT EXISTS frequencies
(
    config_rev   INTEGER     NOT NULL,
    trip_id      VARCHAR(60) NOT NULL,
    start_time   INTEGER     NOT NULL,
    end_time     INTEGER,
    headway_secs INTEGER,
    exact_times  BOOLEAN,
    CONSTRAINT pk_frequencies PRIMARY KEY (config_rev, trip_id, start_time)
);

CREATE TABLE IF NOT EXISTS headways
(
    id                       BIGINT NOT NULL,
    config_rev               INTEGER,
    headway                  DOUBLE PRECISION,
    average                  DOUBLE PRECISION,
    variance                 DOUBLE PRECISION,
    coefficient_of_variation DOUBLE PRECISION,
    num_vehicles             INTEGER,
    creation_time            TIMESTAMP WITHOUT TIME ZONE,
    vehicle_id               VARCHAR(60),
    other_vehicle_id         VARCHAR(60),
    stop_id                  VARCHAR(60),
    trip_id                  VARCHAR(60),
    route_id                 VARCHAR(60),
    first_departure          TIMESTAMP WITHOUT TIME ZONE,
    second_departure         TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_headways PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS holding_times
(
    id                      BIGINT NOT NULL,
    config_rev              INTEGER,
    holding_time            TIMESTAMP WITHOUT TIME ZONE,
    creation_time           TIMESTAMP WITHOUT TIME ZONE,
    vehicle_id              VARCHAR(60),
    stop_id                 VARCHAR(60),
    trip_id                 VARCHAR(60),
    route_id                VARCHAR(60),
    arrival_time            TIMESTAMP WITHOUT TIME ZONE,
    arrival_prediction_used BOOLEAN,
    arrival_used            BOOLEAN,
    has_d1                  BOOLEAN,
    number_prediction_used  INTEGER,
    CONSTRAINT pk_holding_times PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS matches
(
    vehicle_id               VARCHAR(60)                 NOT NULL,
    avl_time                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    config_rev               INTEGER NOT NULL,
    service_id               VARCHAR(255),
    block_id                 VARCHAR(60),
    trip_id                  VARCHAR(60),
    stop_path_index          INTEGER,
    segment_index            INTEGER,
    distance_along_segment   FLOAT,
    distance_along_stop_path FLOAT,
    at_stop                  BOOLEAN,
    CONSTRAINT pk_matches PRIMARY KEY (vehicle_id, avl_time, config_rev)
);

CREATE TABLE IF NOT EXISTS measured_arrival_times
(
    time             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    stop_id          VARCHAR(60)                 NOT NULL,
    route_id         VARCHAR(60),
    route_short_name VARCHAR(60),
    direction_id     VARCHAR(60),
    headsign         VARCHAR(60),
    CONSTRAINT pk_measured_arrival_times PRIMARY KEY (time, stop_id)
);

CREATE TABLE IF NOT EXISTS monitoring_events
(
    time      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    type      VARCHAR(40)                 NOT NULL,
    triggered BOOLEAN,
    message   VARCHAR(512),
    value     DOUBLE PRECISION,
    CONSTRAINT pk_monitoring_events PRIMARY KEY (time, type)
);

CREATE TABLE IF NOT EXISTS prediction_accuracy
(
    id                        BIGINT NOT NULL,
    route_id                  VARCHAR(60),
    route_short_name          VARCHAR(60),
    direction_id              VARCHAR(60),
    stop_id                   VARCHAR(60),
    trip_id                   VARCHAR(60),
    arrival_departure_time    TIMESTAMP WITHOUT TIME ZONE,
    predicted_time            TIMESTAMP WITHOUT TIME ZONE,
    prediction_read_time      TIMESTAMP WITHOUT TIME ZONE,
    prediction_accuracy_msecs INTEGER,
    prediction_source         VARCHAR(60),
    vehicle_id                VARCHAR(60),
    affected_by_wait_stop     BOOLEAN,
    CONSTRAINT pk_prediction_accuracy PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS prediction_events
(
    time                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    vehicle_id           VARCHAR(60)                 NOT NULL,
    event_type           VARCHAR(60)                 NOT NULL,
    avl_time             TIMESTAMP WITHOUT TIME ZONE,
    description          VARCHAR(500),
    route_id             VARCHAR(60),
    route_short_name     VARCHAR(60),
    block_id             VARCHAR(60),
    service_id           VARCHAR(60),
    trip_id              VARCHAR(60),
    stop_id              VARCHAR(60),
    arrival_stop_id      VARCHAR(60),
    departure_stop_id    VARCHAR(60),
    reference_vehicle_id VARCHAR(60),
    arrival_time         TIMESTAMP WITHOUT TIME ZONE,
    departure_time       TIMESTAMP WITHOUT TIME ZONE,
    lat                  DOUBLE PRECISION,
    lon                  DOUBLE PRECISION,
    CONSTRAINT pk_prediction_events PRIMARY KEY (time, vehicle_id, event_type)
);

CREATE TABLE IF NOT EXISTS predictions
(
    id                           BIGINT NOT NULL,
    config_rev                   INTEGER,
    prediction_time              TIMESTAMP WITHOUT TIME ZONE,
    avl_time                     TIMESTAMP WITHOUT TIME ZONE,
    creation_time                TIMESTAMP WITHOUT TIME ZONE,
    vehicle_id                   VARCHAR(60),
    stop_id                      VARCHAR(60),
    trip_id                      VARCHAR(60),
    route_id                     VARCHAR(60),
    affected_by_wait_stop        BOOLEAN,
    is_arrival                   BOOLEAN,
    is_schedule_based_prediction BOOLEAN,
    gtfs_stop_sequence           INTEGER,
    CONSTRAINT pk_predictions PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS routes
(
    config_rev   INTEGER     NOT NULL,
    id           VARCHAR(60) NOT NULL,
    color        VARCHAR(10),
    text_color   VARCHAR(10),
    route_order  INTEGER,
    hidden       BOOLEAN,
    type         VARCHAR(2),
    description  VARCHAR(1024),
    short_name   VARCHAR,
    long_name    VARCHAR,
    name         VARCHAR,
    max_distance DOUBLE PRECISION,
    min_lat      DOUBLE PRECISION,
    max_lat      DOUBLE PRECISION,
    min_lon      DOUBLE PRECISION,
    max_lon      DOUBLE PRECISION,
    CONSTRAINT pk_routes PRIMARY KEY (config_rev, id)
);

CREATE TABLE IF NOT EXISTS stop_path_predictions
(
    id              BIGINT NOT NULL,
    creation_time   TIMESTAMP WITHOUT TIME ZONE,
    prediction_time DOUBLE PRECISION,
    trip_id         VARCHAR(60),
    start_time      INTEGER,
    algorithm       VARCHAR(255),
    stop_path_index INTEGER,
    vehicle_id      VARCHAR(255),
    travel_time     BOOLEAN,
    CONSTRAINT pk_stop_path_predictions PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS stop_paths
(
    config_rev              INTEGER      NOT NULL,
    stop_path_id            VARCHAR(120) NOT NULL,
    trip_pattern_id         VARCHAR(120) NOT NULL,
    stop_id                 VARCHAR(60),
    gtfs_stop_seq           INTEGER,
    last_stop_in_trip       BOOLEAN,
    route_id                VARCHAR(60),
    layover_stop            BOOLEAN      NOT NULL,
    wait_stop               BOOLEAN      NOT NULL,
    schedule_adherence_stop BOOLEAN      NOT NULL,
    break_time              INTEGER,
    path_length             DOUBLE PRECISION,
    max_distance            DOUBLE PRECISION,
    max_speed               DOUBLE PRECISION,
    CONSTRAINT pk_stop_paths PRIMARY KEY (trip_pattern_id, stop_path_id, config_rev)
);

CREATE TABLE IF NOT EXISTS stops
(
    config_rev      INTEGER     NOT NULL,
    id              VARCHAR(60) NOT NULL,
    code            INTEGER,
    name            VARCHAR,
    time_point_stop BOOLEAN,
    layover_stop    BOOLEAN,
    wait_stop       BOOLEAN,
    hidden          BOOLEAN,
    lat             DOUBLE PRECISION,
    lon             DOUBLE PRECISION,
    CONSTRAINT pk_stops PRIMARY KEY (config_rev, id)
);

CREATE TABLE IF NOT EXISTS test
(
    id INTEGER NOT NULL,
    CONSTRAINT pk_test PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS transfers
(
    config_rev        INTEGER     NOT NULL,
    from_stop_id      VARCHAR(60) NOT NULL,
    to_stop_id        VARCHAR(60) NOT NULL,
    transfer_type     VARCHAR(1),
    min_transfer_time INTEGER,
    CONSTRAINT pk_transfers PRIMARY KEY (config_rev, from_stop_id, to_stop_id)
);

CREATE TABLE IF NOT EXISTS travel_times_for_stop_paths
(
    id                         INTEGER NOT NULL,
    config_rev                 INTEGER,
    travel_times_rev           INTEGER,
    stop_path_id               VARCHAR(120),
    travel_time_segment_length FLOAT,
    travel_times_msec          JSON,
    stop_time_msec             INTEGER,
    days_of_week_override      SMALLINT,
    how_set                    VARCHAR(5),
    CONSTRAINT pk_travel_times_for_stop_paths PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS travel_times_for_trip_to_travel_times_for_path
(
    for_path_id INTEGER NOT NULL,
    for_trip_id INTEGER NOT NULL,
    list_index  INTEGER NOT NULL,
    CONSTRAINT pk_travel_times_for_trip_to_travel_times_for_path PRIMARY KEY (for_path_id, list_index)
);

CREATE TABLE IF NOT EXISTS travel_times_for_trips
(
    id                  INTEGER NOT NULL,
    config_rev          INTEGER,
    travel_times_rev    INTEGER,
    trip_pattern_id     VARCHAR(120),
    trip_created_for_id VARCHAR(60),
    CONSTRAINT pk_travel_times_for_trips PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS trip_pattern_to_path
(
    list_index                INTEGER      NOT NULL,
    trip_pattern_config_rev   INTEGER      NOT NULL,
    stop_path_config_rev      INTEGER      NOT NULL,
    stop_path_stop_path_id    VARCHAR(120) NOT NULL,
    stop_path_trip_pattern_id VARCHAR(120) NOT NULL,
    trip_pattern_id           VARCHAR(120) NOT NULL,
    CONSTRAINT pk_trip_pattern_to_path PRIMARY KEY (list_index, trip_pattern_config_rev, trip_pattern_id)
);

CREATE TABLE IF NOT EXISTS trip_patterns
(
    config_rev       INTEGER      NOT NULL,
    id               VARCHAR(120) NOT NULL,
    shape_id         VARCHAR(60),
    headsign         VARCHAR(255),
    direction_id     VARCHAR(60),
    route_id         VARCHAR(60),
    route_short_name VARCHAR(80),
    min_lat          DOUBLE PRECISION,
    max_lat          DOUBLE PRECISION,
    min_lon          DOUBLE PRECISION,
    max_lon          DOUBLE PRECISION,
    CONSTRAINT pk_trip_patterns PRIMARY KEY (config_rev, id)
);

CREATE TABLE IF NOT EXISTS trips
(
    config_rev             INTEGER     NOT NULL,
    trip_id                VARCHAR(60) NOT NULL,
    start_time             INTEGER     NOT NULL,
    trip_short_name        VARCHAR(60),
    end_time               INTEGER,
    direction_id           VARCHAR(60),
    route_id               VARCHAR(60),
    route_short_name       VARCHAR(60),
    travelTimes_id         INTEGER,
    no_schedule            BOOLEAN,
    exact_times_headway    BOOLEAN,
    service_id             VARCHAR(60),
    headsign               VARCHAR(255),
    block_id               VARCHAR(60),
    shape_id               VARCHAR(60),
    tripPattern_config_rev INTEGER,
    tripPattern_id         VARCHAR(120),
    CONSTRAINT pk_trips PRIMARY KEY (config_rev, trip_id, start_time)
);

CREATE TABLE IF NOT EXISTS vehicle_configs
(
    id                    VARCHAR(60) NOT NULL,
    name                  VARCHAR(255),
    type                  INTEGER,
    description           VARCHAR,
    tracker_id            VARCHAR(60),
    capacity              INTEGER,
    crush_capacity        INTEGER,
    non_passenger_vehicle BOOLEAN,
    CONSTRAINT pk_vehicle_configs PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS vehicle_events
(
    time                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    vehicle_id           VARCHAR(60)                 NOT NULL,
    event_type           VARCHAR(60)                 NOT NULL,
    avl_time             TIMESTAMP WITHOUT TIME ZONE,
    description          VARCHAR(500),
    predictable          BOOLEAN,
    became_unpredictable BOOLEAN,
    supervisor           VARCHAR(60),
    route_id             VARCHAR(60),
    route_short_name     VARCHAR(60),
    block_id             VARCHAR(60),
    service_id           VARCHAR(60),
    trip_id              VARCHAR(60),
    stop_id              VARCHAR(60),
    lat                  DOUBLE PRECISION,
    lon                  DOUBLE PRECISION,
    CONSTRAINT pk_vehicle_events PRIMARY KEY (time, vehicle_id, event_type)
);

CREATE TABLE IF NOT EXISTS vehicle_states
(
    vehicle_id                       VARCHAR(60)                 NOT NULL,
    avl_time                         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    block_id                         VARCHAR(60),
    trip_id                          VARCHAR(60),
    trip_short_name                  VARCHAR(60),
    route_id                         VARCHAR(60),
    route_short_name                 VARCHAR(80),
    schedule_adherence_msec          INTEGER,
    schedule_adherence               VARCHAR(50),
    schedule_adherence_within_bounds BOOLEAN,
    is_delayed                       BOOLEAN,
    is_layover                       BOOLEAN,
    is_predictable                   BOOLEAN,
    is_wait_stop                     BOOLEAN,
    is_for_sched_based_predictions   BOOLEAN,
    CONSTRAINT pk_vehicle_states PRIMARY KEY (vehicle_id, avl_time)
);

CREATE TABLE IF NOT EXISTS vehicle_to_block_configs
(
    id              BIGINT                      NOT NULL,
    vehicle_id      VARCHAR(60)                 NOT NULL,
    assignment_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    block_id        VARCHAR(60),
    trip_id         VARCHAR(60),
    valid_from      TIMESTAMP WITHOUT TIME ZONE,
    valid_to        TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_vehicle_to_block_configs PRIMARY KEY (id, vehicle_id)
);

CREATE TABLE IF NOT EXISTS web_agencies
(
    agencyId            VARCHAR(60) NOT NULL,
    hostName            VARCHAR(120),
    active              BOOLEAN,
    dbName              VARCHAR(60),
    dbType              VARCHAR(60),
    dbHost              VARCHAR(120),
    dbUserName          VARCHAR(60),
    dbEncryptedPassword VARCHAR(60),
    CONSTRAINT pk_web_agencies PRIMARY KEY (agencyId)
);

ALTER TABLE trip_pattern_to_path
    ADD CONSTRAINT uc_trip_pattern_to_path_stcoreststpaidstpatrpaid UNIQUE (stop_path_config_rev,
                                                                            stop_path_stop_path_id,
                                                                            stop_path_trip_pattern_id);

CREATE INDEX ArrivalsDeparturesRouteTimeIndex ON arrivals_departures (route_short_name, time);

CREATE INDEX ArrivalsDeparturesTimeIndex ON arrivals_departures (time);

CREATE INDEX AvlReportsTimeIndex ON avl_reports (time);

CREATE INDEX AvlTimeIndex ON matches (avl_time);

CREATE INDEX HeadwayIndex ON headways (creation_time);

CREATE INDEX HoldingTimeIndex ON holding_times (creation_time);

CREATE INDEX MeasuredArrivalTimesIndex ON measured_arrival_times (time);

CREATE INDEX MonitoringEventsTimeIndex ON monitoring_events (time);

CREATE INDEX PredictionAccuracyTimeIndex ON prediction_accuracy (arrival_departure_time);

CREATE INDEX PredictionEventsTimeIndex ON prediction_events (time);

CREATE INDEX PredictionTimeIndex ON predictions (creation_time);

CREATE INDEX StopPathPredictionTimeIndex ON stop_path_predictions (trip_id, stop_path_index);

CREATE INDEX TravelTimesRevIndex ON travel_times_for_trips (travel_times_rev);

CREATE INDEX VehicleEventsTimeIndex ON vehicle_events (time);

CREATE INDEX VehicleStateAvlTimeIndex ON vehicle_states (avl_time);

ALTER TABLE trips
    ADD CONSTRAINT FK_TRIPS_ON_TRAVELTIMES FOREIGN KEY (travelTimes_id) REFERENCES travel_times_for_trips (id);

ALTER TABLE trips
    ADD CONSTRAINT FK_TRIPS_ON_TRCORETRID FOREIGN KEY (tripPattern_config_rev, tripPattern_id) REFERENCES trip_patterns (config_rev, id);

ALTER TABLE block_to_trip
    ADD CONSTRAINT fk_block_to_trip_on_block FOREIGN KEY (block_config_rev, block_block_id, block_service_id) REFERENCES blocks (config_rev, block_id, service_id);

ALTER TABLE block_to_trip
    ADD CONSTRAINT fk_block_to_trip_on_trip FOREIGN KEY (trips_config_rev, trips_trip_id, trips_start_time) REFERENCES trips (config_rev, trip_id, start_time);

ALTER TABLE stoppath_locations
    ADD CONSTRAINT fk_stoppath_locations_on_stop_path FOREIGN KEY (stoppath_config_rev, stoppath_stop_path_id, stoppath_trip_pattern_id) REFERENCES stop_paths (config_rev, stop_path_id, trip_pattern_id);

ALTER TABLE travel_times_for_trip_to_travel_times_for_path
    ADD CONSTRAINT fk_tratimfortritotratimforpat_on_travel_times_for_stop_path FOREIGN KEY (for_trip_id) REFERENCES travel_times_for_stop_paths (id);

ALTER TABLE travel_times_for_trip_to_travel_times_for_path
    ADD CONSTRAINT fk_tratimfortritotratimforpat_on_travel_times_for_trip FOREIGN KEY (for_path_id) REFERENCES travel_times_for_trips (id);

ALTER TABLE trip_scheduled_times_list
    ADD CONSTRAINT fk_trip_scheduledtimeslist_on_trip FOREIGN KEY (trip_config_rev, trip_trip_id, trip_start_time) REFERENCES trips (config_rev, trip_id, start_time);

ALTER TABLE trip_pattern_to_path
    ADD CONSTRAINT fk_tripatern_to_path_on_stop_path FOREIGN KEY (stop_path_trip_pattern_id, stop_path_stop_path_id, stop_path_config_rev) REFERENCES stop_paths (trip_pattern_id, stop_path_id, config_rev);

ALTER TABLE trip_pattern_to_path
    ADD CONSTRAINT fk_tripatern_to_path_on_trip_pattern FOREIGN KEY (trip_pattern_id, trip_pattern_config_rev) REFERENCES trip_patterns (id, config_rev);