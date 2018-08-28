-- users lookup table
DROP TABLE IF EXISTS WEB_USERS;
CREATE TABLE WEB_USERS (user_id int, registered_At long, username varchar, first_name varchar, last_name varchar, city varchar, level varchar) with (key='user_id', kafka_topic = 'clickstream_users', value_format = 'json');

-- Clickstream enriched with user account data
DROP STREAM IF EXISTS customer_clickstream;
CREATE STREAM customer_clickstream WITH (PARTITIONS=2) AS SELECT userid, u.first_name, u.last_name, u.level, time, ip, request, status, agent FROM clickstream c LEFT JOIN web_users u ON c.userid = u.user_id;

-- Find error views by important users
DROP STREAM IF EXISTS platinum_customers_with_errors
CREATE stream platinum_customers_with_errors WITH (PARTITIONS=2) AS seLECT * FROM customer_clickstream WHERE status > 400 AND level = 'Platinum';

-- Find error views by important users in one shot
DROP STREAM IF EXISTS platinum_errors;
CREATE STREAM platinum_errors WITH (PARTITIONS=2) AS SELECT userid, u.first_name, u.last_name, u.city, u.level, time, ip, request, status, agent FROM clickstream c LEFT JOIN web_users u ON c.userid = u.user_id WHERE status > 400 AND level = 'Platinum';
--
---- Trend of errors from important users
DROP TABLE IF EXISTS platinum_page_errors_per_5_min;
CREATE TABLE platinum_errors_per_5_min AS SELECT userid, first_name, last_name, city, count(*) AS running_count FROM platinum_errors WINDOW TUMBLING (SIZE 5 MINUTE) WHERE request LIKE '%html%' GROUP BY userid, first_name, last_name, city;
