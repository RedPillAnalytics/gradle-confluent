import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Slf4j
@Title("Check basic configuration")
class DeployTest extends Specification {

   @Shared
   File projectDir, buildDir, resourcesDir, buildFile, artifact

   @Shared
   def result, tasks, taskList

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/simple-deploy")
      buildDir = new File(projectDir, 'build')
      buildFile = new File(projectDir, 'build.gradle')
      //artifact = new File(buildDir, 'distributions/build-test-pipeline.zip')
      taskList = ['functionCopy', 'pipelineExtract', 'deploy']

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
            
            dependencies {
               archives group: 'com.redpillanalytics', name: 'ksql-functions', version: '+'
               archives group: 'com.redpillanalytics', name: 'test-pipeline', version: '+'
            }
            
            confluent.functionArtifactName = 'ksql-functions.jar'
        """)
   }

   def "Deploy test using mavenLocal()"() {

      given:

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'deploy')
              .withPluginClasspath()
              .build()

      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":deploy").outcome.toString())
      tasks.collect { it - ' SKIPPED' } == taskList
   }
}
