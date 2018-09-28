package streams

import groovy.util.logging.Slf4j
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.ForeachAction
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Serialized

@Slf4j
class TestClass {

   static void main(String[] args) {

      log.warn "Executing main..."

   }

}
