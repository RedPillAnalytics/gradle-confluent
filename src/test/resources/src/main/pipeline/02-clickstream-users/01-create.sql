-- lets the windows accumulate more data
set 'commit.interval.ms'='2000';
set 'cache.max.bytes.buffering'='10000000';
set 'auto.offset.reset'='earliest';

-- users lookup table
CREATE TABLE WEB_USERS (user_id int, registered_At long, username varchar, first_name varchar, last_name varchar, city varchar, level varchar) 
with (key='user_id', kafka_topic = 'clickstream_users', value_format = 'json');
