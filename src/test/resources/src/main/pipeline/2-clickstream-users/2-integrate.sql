 -- number of events per minute - think about key-for-distribution-purpose - shuffling etc - shouldnt use 'userid'
CREATE table events_per_min AS SELECT userid, count(*) AS events 
FROM clickstream window TUMBLING (size 60 second) 
GROUP BY userid;

-- 4. BUILD PAGE_VIEWS
CREATE TABLE pages_per_min AS 
SELECT userid, count(*) AS pages 
FROM clickstream WINDOW HOPPING (size 60 second, advance by 5 second) 
WHERE request like '%html%' 
GROUP BY userid;

-- Use 'HAVING' Filter to show ERROR codes > 400 where count > 5
CREATE TABLE ERRORS_PER_MIN_ALERT AS 
SELECT status, count(*) AS errors 
FROM clickstream window HOPPING ( size 30 second, advance by 20 second) 
WHERE status > 400 
GROUP BY status HAVING count(*) > 5 
AND count(*) is not NULL;

CREATE table ERRORS_PER_MIN AS 
SELECT status, count(*) AS errors 
FROM clickstream window HOPPING ( size 60 second, advance by 5  second) 
WHERE status > 400 GROUP BY status;

--Join using a STREAM
CREATE STREAM ENRICHED_ERROR_CODES AS 
SELECT code, definition 
FROM clickstream 
LEFT JOIN clickstream_codes 
ON clickstream.status = clickstream_codes.code;

-- Aggregate (count&groupBy) using a TABLE-Window
CREATE TABLE ENRICHED_ERROR_CODES_COUNT AS
SELECT code, definition, COUNT(*) AS count 
FROM ENRICHED_ERROR_CODES WINDOW TUMBLING (size 30 second) 
GROUP BY code, definition 
HAVING COUNT(*) > 1;

-- Clickstream enriched with user account data
CREATE STREAM customer_clickstream WITH (PARTITIONS=2) AS 
SELECT userid, u.first_name, u.last_name, u.level, time, ip, request, status, agent 
FROM clickstream c 
LEFT JOIN web_users u 
ON c.userid = u.user_id;

-- View IP, username and City Versus web-site-activity (hits)
CREATE STREAM USER_CLICKSTREAM AS 
SELECT userid, u.username, ip, u.city, request, status, bytes 
FROM clickstream c 
LEFT JOIN web_users u 
ON c.userid = u.user_id;
