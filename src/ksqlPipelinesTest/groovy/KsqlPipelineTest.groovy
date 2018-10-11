import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
class KsqlPipelineTest extends Specification {

   @Shared
   def ksqlRest = new KsqlRest()

   def "getSourceDescription() returns the appropriate results"() {

      when:
      def result = ksqlRest.getSourceDescription('clickstream')

      then:
      log.warn "result: ${result.toString()}"
      result
   }

   def "getReadQueries() returns the appropriate results"() {

      when:
      def result = ksqlRest.getReadQueries('clickstream')

      then:
      log.warn "result: ${result.toString()}"
      result
   }

   def "getWriteQueries() returns the appropriate results"() {

      when:
      def result = ksqlRest.getWriteQueries('clickstream')

      then:
      log.warn "result: ${result.toString()}"
      result
   }
}
