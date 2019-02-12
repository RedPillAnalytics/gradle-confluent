import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Slf4j
@Title("Check basic configuration")
class DeployTest extends Specification {

   @Shared
   File projectDir, buildDir, resourcesDir, buildFile, settingsFile, artifact

   @Shared
   def result, tasks, taskList

   @Shared
   String projectName = 'simple-deploy'

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/$projectName")
      buildDir = new File(projectDir, 'build')
      artifact = new File(buildDir, 'distributions/simple-deploy-pipeline.zip')
      taskList = ['functionCopy', 'pipelineExtract', 'pipelineDeploy', 'deploy']

      resourcesDir = new File('src/test/resources')

      new AntBuilder().copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }

      settingsFile = new File(projectDir, 'settings.gradle').write("""rootProject.name = '$projectName'""")

      buildFile = new File(projectDir, 'build.gradle').write("""
            plugins {
               id 'com.redpillanalytics.gradle-confluent'
               id 'maven-publish'
            }
            publishing {
              repositories {
                mavenLocal()
              }
            }
            group = 'com.redpillanalytics'
            version = '1.0.0'
            
            repositories {
               jcenter()
               mavenLocal()
               maven {
                  name 'test'
                  url 's3://maven.redpillanalytics.com/demo/maven2'
                  authentication {
                     awsIm(AwsImAuthentication)
                  }
              }
            }
            
            dependencies {
               archives group: 'com.redpillanalytics', name: 'simple-build', version: '+'
               archives group: 'com.redpillanalytics', name: 'simple-build-pipeline', version: '+'
            }

            confluent {
               functionPattern 'simple-build'
            }
        """)
   }

   def "Deploy test S3"() {

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
