FROM confluentinc/ksql-clickstream-demo:5.0.0

EXPOSE 3000

ADD docker-files/datagen-init.sh /

RUN   apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y vim less \
    && echo "" >> /etc/kafka/server.properties \
    && echo "advertised.listeners=PLAINTEXT://localhost:9092" >> /etc/kafka/server.properties \
    && echo "advertised.host.name=localhost" >> /etc/kafka/server.properties \
    && echo "rest.port=18083" >> /etc/schema-registry/connect-avro-standalone.properties \
    && echo "ksql.extension.dir=/etc/ksql/ext" >> /etc/ksql/ksql-server.properties \
    && mkdir /etc/ksql/ext \
    && mkdir /share/java/kafka/plugins

ENTRYPOINT confluent start && ./datagen-init.sh && bash
