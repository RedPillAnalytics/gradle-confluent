-- users lookup table
CREATE TABLE WEB_USERS (user_id int, registered_At bigint, username varchar, first_name varchar, last_name varchar, city varchar, level varchar)
with (key='user_id', kafka_topic = 'clickstream_users', value_format = 'json');
