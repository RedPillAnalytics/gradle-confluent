import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Slf4j
@Title("Test that a :build functions successfully")
class BuildTest extends Specification {

   @Shared
   File projectDir, buildDir, resourcesDir, buildFile, artifact

   @Shared
   def result, tasks, taskList

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/simple-build")
      buildDir = new File(projectDir, 'build')
      buildFile = new File(projectDir, 'build.gradle')
      artifact = new File(buildDir, 'distributions/test-pipeline.zip')
      taskList = ['loadConfig',
                  'clean',
                  'assemble',
                  'check',
                  'createScripts',
                  'pipelineZip',
                  'build',
                  'generatePomFileForPipelinePublication',
                  'publishPipelinePublicationToMavenLocalRepository',
                  'publish']


      resourcesDir = new File('src/test/resources')

      new AntBuilder().copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }

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
            archivesBaseName = 'test'
            group = 'com.redpillanalytics'
            version = '1.0.0'
            
            repositories {
              mavenLocal()
            }
        """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'clean', 'build', 'publish', '--rerun-tasks')
              .withPluginClasspath()
              .build()

      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }

      log.warn result.getOutput()
   }

   def "All tasks run and in the correct order"() {

      given:
      "Gradle build runs"

      expect:
      tasks.collect { it - ' SKIPPED' } == taskList
   }

   @Unroll
   def "The execution of :#task is successful"() {

      when:
      "Gradle build runs"

      then:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":$task").outcome.toString())

      where:
      task << taskList
   }
}
