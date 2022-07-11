--BUILD STATUS_CODES
CREATE SOURCE TABLE other_status_codes (code int primary key, definition varchar)
    with (kafka_topic = 'other_status_codes', value_format = 'json');