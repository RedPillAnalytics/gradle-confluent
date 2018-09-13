import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.omg.PortableInterceptor.SUCCESSFUL
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Slf4j
@Title("Test for CreateScriptsTask")
class CreateScriptsTest extends Specification {

   @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
   File buildFile

   @Shared
   File resourcesDir = new File('src/test/resources')

   @Shared
   def result

   def setup() {
      new AntBuilder().copy(todir: testProjectDir.root) {
         fileset(dir: resourcesDir)
      }
      buildFile = testProjectDir.newFile('build.gradle')
   }

   def "Create script files with defaults"() {

      given:

      buildFile.write("""
            plugins {
                id 'com.redpillanalytics.gradle-confluent'
            }
        """)

      when:
      result = GradleRunner.create()
              .withProjectDir(testProjectDir.root)
              .withArguments('-Si', 'createScripts', '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn "tasks: $result.tasks"
      log.info result.task(":createScripts").outcome.dump()
      log.warn "I'm executing with original"


      then:
      result.task(":createScripts").outcome.toString() == 'SUCCESS'
   }

   def "Create script files with --reverse-drops-disabled"() {

      given:
      buildFile.write("""
            plugins {
                id 'com.redpillanalytics.gradle-confluent'
            }
        """)

      when:
      result = GradleRunner.create()
              .withProjectDir(testProjectDir.root)
              .withArguments('-Si', 'createScripts', '--reverse-drops-disabled', '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn "tasks: $result.tasks"
      log.info result.task(":createScripts").outcome.dump()
      log.warn "I'm executing with reverse"

      then:
      result.task(":createScripts").outcome.toString() == 'SUCCESS'
   }

}
