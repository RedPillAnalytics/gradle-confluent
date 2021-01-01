--SOURCE of ClickStream
CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar)
with (kafka_topic = 'clickstream', value_format = 'json');

--BUILD STATUS_CODES
CREATE TABLE clickstream_codes (code int primary key, definition varchar)
with (kafka_topic = 'clickstream_codes', value_format = 'json');
