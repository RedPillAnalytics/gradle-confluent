
----------------------------------------------------------------------------------------------------------------------------
-- A series of basic clickstream-analytics
--
-- Min, Max, UDFs etc
----------------------------------------------------------------------------------------------------------------------------

-- number of events per minute - think about key-for-distribution-purpose - shuffling etc - shouldnt use 'userid'
--@DeleteTopic
CREATE table events_per_min AS
SELECT userid, count(*) AS events
FROM clickstream window TUMBLING (size 60 second)
GROUP BY userid
emit changes;

-- BUILD PAGE_VIEWS
CREATE TABLE pages_per_min AS
SELECT userid, count(*) AS pages
FROM clickstream WINDOW HOPPING (size 60 second, advance by 5 second)
WHERE request like '%html%'
GROUP BY userid
emit changes;

--Join using a STREAM
CREATE STREAM ENRICHED_ERROR_CODES AS SELECT code, definition
FROM clickstream
LEFT JOIN clickstream_codes
ON clickstream.status = clickstream_codes.code
emit changes;

