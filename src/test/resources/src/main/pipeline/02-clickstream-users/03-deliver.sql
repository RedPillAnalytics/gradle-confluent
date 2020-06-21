-- Aggregate (count&groupBy) using a TABLE-Window
CREATE TABLE USER_IP_ACTIVITY AS \
SELECT username, ip, city, COUNT(*) AS count \
FROM USER_CLICKSTREAM WINDOW TUMBLING (size 60 second) \
GROUP BY username, ip, city \
HAVING COUNT(*) > 1;

-- Sessionisation using IP addresses - 300 seconds of inactivity expires the session
CREATE TABLE CLICK_USER_SESSIONS AS \
SELECT username, count(*) AS events \
FROM USER_CLICKSTREAM window SESSION (300 second) \
GROUP BY username;
