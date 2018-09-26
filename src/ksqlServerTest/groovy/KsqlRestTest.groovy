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
      def result = ksqlRest.getKsqlExtensionPath()

      then:
      result

   }

   def "KSQL extension directory file is returned"() {

      when:
      def result = ksqlRest.getKsqlExtensionDir()

      then:
      result

   }
}
