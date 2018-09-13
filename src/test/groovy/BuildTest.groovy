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
           buildFile = new File(resourcesDir, 'build.gradle')
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
              .withArguments('-Si', 'clean', 'build','--rerun-tasks')
              .withPluginClasspath()
              .build()

      indexedResultOutput = result.output.readLines()

      log.warn result.output

   }

   def cleanupSpec() {

      buildFile.delete()
   }

   def "Expect to generate the deployment files"() {

      given: "a gradle execution running the :build task"
      def zipFile = new File(resourcesDir,'build/distributions/resources-pipeline.zip')

      expect:
      zipFile.exists()

   }

   @Unroll
   def "Executing :build contains :#task"() {

      given: "a gradle execution running the :build task"

      expect:
      result.output.contains("BUILD SUCCESSFUL")
      //result.output.contains(":$task")

      where:
      task << ['build', 'createScripts', 'pipelineZip']
   }

//   @Unroll
//   def "Executing :build ensures :#firstTask runs before :#secondTask"() {
//
//      given: "a gradle execution running the :build task"
//
//      expect: "the index of :firstTask is lower than the index of :secondTask"
//      indexedResultOutput.findIndexOf { it =~ /(:$firstTask)( SKIPPED)/ } < indexedResultOutput.findIndexOf {
//         it =~ /(:$secondTask)( SKIPPED)/
//      }
//
//      where:
//
//      firstTask << ['clean', 'deployScript', 'buildPipeline']
//      secondTask << ['build', 'buildPipeline', 'build']
//   }

}
