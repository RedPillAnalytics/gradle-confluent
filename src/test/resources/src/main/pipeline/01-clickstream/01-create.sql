SET 'auto.offset.reset'='earliest';
--SOURCE of ClickStream
CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) 
with (kafka_topic = 'clickstream', value_format = 'json');

--BUILD STATUS_CODES
-- it's fne to have multiple comment lines
-- everybody's doing it
CREATE TABLE clickstream_codes (code int, definition varchar) 
with (key='code', kafka_topic = 'clickstream_codes', value_format = 'json');