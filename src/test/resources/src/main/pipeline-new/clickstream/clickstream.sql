--SOURCE of ClickStream
CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) 
with (kafka_topic = 'clickstream', value_format = 'json');
