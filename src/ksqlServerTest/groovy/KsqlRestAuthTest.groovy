import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
class KsqlRestAuthTest extends Specification {

   @Shared
   String pipelineEndpoint = System.getProperty("pipelineEndpoint") ?: 'http://localhost:8088'
   String username = System.getProperty("pipelineUsername") ?: 'test'
   String password = System.getProperty("pipelinePassword") ?: 'test'

   @Shared
   def ksqlRest = new KsqlRest(restUrl: pipelineEndpoint, username: username, password: password)

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
