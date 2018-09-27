import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
class KsqlRestTest extends Specification {

   @Shared
   def ksqlRest = new KsqlRest()

   def "KSQL Server properties fetched"() {

      when:
      def result = ksqlRest.getProperties()

      then:
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
}
