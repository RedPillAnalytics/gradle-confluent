SET 'auto.offset.reset'='earliest';

CREATE TABLE streama (id BIGINT primary key, userid varchar, name varchar)
with (kafka_topic = 'streama', value_format = 'json', PARTITIONS=1, REPLICAS=1);

SET 'auto.offset.reset'='latest';

CREATE TABLE streamb (id BIGINT primary key, userid varchar, name varchar)
with (kafka_topic = 'streamb', value_format = 'json', PARTITIONS=1, REPLICAS=1);

UNSET 'auto.offset.reset';

CREATE TABLE streamc (id BIGINT primary key, userid varchar, name varchar)
with (kafka_topic = 'streamc', value_format = 'json', PARTITIONS=1, REPLICAS=1);
