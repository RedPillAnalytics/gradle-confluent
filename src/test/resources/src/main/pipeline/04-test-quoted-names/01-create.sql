--@DeleteTopic
CREATE STREAM "testStream" (
    user_id int
) WITH (
    KAFKA_TOPIC = 'testStream',
    VALUE_FORMAT = 'JSON',
    PARTITIONS = 1
);