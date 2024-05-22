-- removing faulty constraints definitions
ALTER TABLE travel_times_for_trip_to_travel_times_for_path
    DROP CONSTRAINT fk_tratimfortritotratimforpat_on_travel_times_for_stop_path;

ALTER TABLE travel_times_for_trip_to_travel_times_for_path
    DROP CONSTRAINT fk_tratimfortritotratimforpat_on_travel_times_for_trip;

-- recreating new constraints
ALTER TABLE travel_times_for_trip_to_travel_times_for_path
    ADD CONSTRAINT fk_tratimfortritotratimforpat_on_travel_times_for_stop_path FOREIGN KEY (for_path_id) REFERENCES travel_times_for_stop_paths (id);

ALTER TABLE travel_times_for_trip_to_travel_times_for_path
    ADD CONSTRAINT fk_tratimfortritotratimforpat_on_travel_times_for_trip FOREIGN KEY (for_trip_id) REFERENCES travel_times_for_trips (id);