import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.spock.Testcontainers
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

@Slf4j
@Testcontainers
class KsqlRestTest extends Specification {

   @Shared
   DockerComposeContainer kafka = new DockerComposeContainer(new File("docker-compose.yml"))
           .withExposedService('ksql-server', 8088, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))

   @Shared
   def ksqlRest

   def setupSpec() {
      String url = "http://${kafka.getServiceHost('ksql-server', 8088)}:${kafka.getServicePort('ksql-server', 8088)}"
      log.warn "KSQL url: $url"
      ksqlRest = new KsqlRest(baseUrl: url)
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
      def url = ksqlRest.getRestUrl()

      then:
      url
   }

   def "List of topics returned"() {

      when:
      def topics = ksqlRest.getTopics()

      then:
      topics
   }

   @Ignore
   def "List of streams returned"() {

      when:
      def topics = ksqlRest.getStreams()

      then:
      topics
   }
}
