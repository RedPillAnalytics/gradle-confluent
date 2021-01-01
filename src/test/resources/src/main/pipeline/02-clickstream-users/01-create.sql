-- users lookup table
CREATE TABLE WEB_USERS (user_id int primary key, registered_At bigint, username varchar, first_name varchar, last_name varchar, city varchar, level varchar)
with (kafka_topic = 'clickstream_users', value_format = 'json');
