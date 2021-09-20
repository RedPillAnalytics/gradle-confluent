import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

@Slf4j
@Testcontainers
class KsqlRestTest extends Specification {
   @Shared
   def ksqlRest

   @Shared
   DockerComposeContainer environment =
           new DockerComposeContainer<>(new File('docker-compose.yml'))
                   .withServices("zookeeper", "kafka", "ksqldb-server")
                   .withExposedService("ksqldb-server", 8088, Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))
                   .withLocalCompose(true)

   def setupSpec() {
      ksqlRest = new KsqlRest(restUrl: ("http://${environment.getServiceHost('ksqldb-server', 8088)}:${environment.getServicePort('ksqldb-server', 8088)}".toString()))
   }

   def "KSQL Server properties fetched"() {

      when:
      def result = ksqlRest.getProperties()

      then:
      log.warn "result: ${result.toString()}"
      result
   }

   def "KSQL extension directory path is returned"() {

      when:
      def path = ksqlRest.getExtensionPath()

      then:
      path
   }

   def "KSQL extension directory file is returned"() {

      when:
      def dir = ksqlRest.getExtensionDir()

      then:
      dir
   }

   def "KSQL REST URL is returned"() {

      when:
      def url = ksqlRest.getSchemaRegistry()

      then:
      url
   }

   def "List of topics returned"() {

      when:
      def topics = ksqlRest.getTopics()

      then:
      topics
   }
}
