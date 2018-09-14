import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Slf4j
@Title("Check basic configuration")
class BuildTest extends Specification {

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

   @Unroll
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
            archivesBaseName = 'build-test'
        """)

      when:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'clean', 'build', '--rerun-tasks')
              .withPluginClasspath()
              .build()

      // produces a nice clean list of tasks in the order they ran
      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }


      then:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":$task").outcome.toString())
      artifact.exists()
      tasks.indexOf(firstTask) < tasks.indexOf(secondTask)

      where:
      task << ['build', 'createScripts', 'pipelineZip']
      firstTask << ['clean', 'createScripts', 'pipelineZip']
      secondTask << ['build', 'pipelineZip', 'build']
   }
}
