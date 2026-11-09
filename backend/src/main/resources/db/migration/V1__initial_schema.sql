-- Business tables are introduced by their own migrations.
CREATE TABLE orderops_schema_marker (
    marker_id integer PRIMARY KEY,
    description text NOT NULL
);
INSERT INTO orderops_schema_marker (marker_id, description)
VALUES (1, 'OrderOps initial schema');
