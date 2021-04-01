
----------------------------------------------------------------------------------------------------------------------------
-- A series of basic clickstream-analytics
--
-- Min, Max, UDFs etc
----------------------------------------------------------------------------------------------------------------------------
-- Aggregate (count&groupBy) using a TABLE-Window
CREATE TABLE ENRICHED_ERROR_CODES_COUNT 
WITH (KEY_FORMAT='JSON')
AS
SELECT code, definition, COUNT(*) AS count
FROM ENRICHED_ERROR_CODES WINDOW TUMBLING (size 30 second)
GROUP BY code, definition
HAVING COUNT(*) > 1
emit changes;
