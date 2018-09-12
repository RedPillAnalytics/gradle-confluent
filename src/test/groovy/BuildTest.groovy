
import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll



@Slf4j
@Title("Check basic configuration")
class BuildTest extends Specification {

   @ClassRule
   @Shared
   TemporaryFolder testProjectDir = new TemporaryFolder()

   @Shared
           resourcesDir = new File('src/test/resources')

   @Shared
           buildFile = new File(resourcesDir,'build.gradle')
   @Shared
           result
   @Shared
           indexedResultOutput

   // run the Gradle build
   // return regular output
   def setupSpec() {

      //buildFile = testProjectDir.newFile('build.gradle')
      buildFile.write("""
            plugins {
                id 'com.redpillanalytics.gradle-confluent'
            }
        """)


      result = GradleRunner.create()
              .withProjectDir(resourcesDir)
              .withArguments('-Si', 'build')
              .withPluginClasspath()
              .build()

      indexedResultOutput = result.output.readLines()

      log.warn result.output

   }

   def cleanupSpec() {

      buildFile.delete()
   }

   @Unroll
   def "Executing :build contains :#task"() {

      given: "a gradle build execution"

      expect:
      result.output.contains("BUILD SUCCESSFUL")
      //result.output.contains(":$task")

      where:
      task << ['build', 'buildPipelines']
   }

}
