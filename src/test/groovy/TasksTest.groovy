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
class TasksTest extends Specification {

   @Shared
   File projectDir
   File buildDir

   @Shared
   File buildFile

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
   }

   def cleanup() {
      buildDir.delete()
   }

   @Unroll
   def "Executing :tasks contains :#task"() {

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
            archivesBaseName = 'build-test'
        """)

      when:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'tasks', '--all', 'showConfiguration')
              .withPluginClasspath()
              .build()

      // produces a nice clean list of tasks in the order they ran
      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }
      log.warn result.output


      then:
      result.output.contains(task)
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":tasks").outcome.toString())

      where:
      task << ['build', 'createScripts', 'pipelineZip','publish']
   }
}
