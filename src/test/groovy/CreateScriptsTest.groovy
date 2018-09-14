import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Slf4j
@Title("Test for CreateScriptsTask")
class CreateScriptsTest extends Specification {

   @Shared
   File projectDir
   File buildDir

   @Shared
   File buildFile, artifact

   @Shared
   File resourcesDir = new File('src/test/resources')

   @Shared
   def result

   @Shared
   def tasks

   def setup() {
      projectDir = File.createTempDir()
      buildDir = new File(projectDir, 'build')

      new AntBuilder().copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }
      buildFile = new File(projectDir, 'build.gradle')
      artifact = new File(buildDir, 'distributions/build-test-pipeline.zip')
   }

   def cleanup() {
      buildDir.delete()
   }

   def "Create script files with defaults"() {

      given:

      buildFile.write("""
            plugins {
               id 'com.redpillanalytics.gradle-confluent'
               id 'maven-publish'
            }
            publishing {
              repositories {
                mavenLocal()
              }
            }
            //archivesBaseName 'scripts-sorted'
        """)

      when:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'createScripts', '--rerun-tasks', 'publish')
              .withPluginClasspath()
              .build()

      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }


      then:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":publish").outcome.toString())
   }

   def "Create script files with --reverse-drops-disabled"() {

      given:
      buildFile.write("""
            plugins {
               id 'com.redpillanalytics.gradle-confluent'
               id 'maven-publish'
            }
            publishing {
              repositories {
                mavenLocal()
              }
            }
            //archivesBaseName 'scripts-reversed'
        """)

      when:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'createScripts', '--reverse-drops-disabled', '--rerun-tasks', 'publish')
              .withPluginClasspath()
              .build()

      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }

      then:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":publish").outcome.toString())

   }

}
