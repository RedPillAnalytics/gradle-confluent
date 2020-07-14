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
   File projectDir, buildDir, settingsFile, resourcesDir, buildFile, artifact

   @Shared
   def result, tasks, taskList

   @Shared
   String projectName = 'run-tasks'

   @Shared
   String pipelineEndpoint = System.getProperty("pipelineEndpoint") ?: 'http://localhost:8088'

   @Shared
   String kafkaServers = System.getProperty("kafkaServers") ?: 'localhost:9092'

   @Shared
   String analyticsVersion = System.getProperty("analyticsVersion")

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/$projectName")
      buildDir = new File(projectDir, 'build')
      artifact = new File(buildDir, 'distributions/build-test-pipeline.zip')
      taskList = ['clean', 'assemble', 'check', 'pipelineScript', 'pipelineZip', 'build']

      resourcesDir = new File('src/test/resources')

      new AntBuilder().copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }

      settingsFile = new File(projectDir, 'settings.gradle').write("""rootProject.name = '$projectName'""")

      buildFile = new File(projectDir, 'build.gradle').write("""
               |plugins {
               |  id 'com.redpillanalytics.gradle-confluent'
               |  id "com.redpillanalytics.gradle-analytics" version "$analyticsVersion"
               |  id 'maven-publish'
               |  id 'application'
               |}
               |
               |publishing {
               |  repositories {
               |    mavenLocal()
               |  }
               |}
               |archivesBaseName = 'test'
               |group = 'com.redpillanalytics'
               |version = '1.0.0'
               |
               |repositories {
               |  jcenter()
               |  mavenLocal()
               |  maven {
               |     name 'test'
               |     url '${resourcesDir}/maven'
               |  }
               |}
               |
               |dependencies {
               |   archives group: 'com.redpillanalytics', name: 'simple-build', version: '+'
               |   archives group: 'com.redpillanalytics', name: 'simple-build-pipeline', version: '+'
               |}
               |
               |confluent {
               |  pipelineEndpoint = '$pipelineEndpoint'
               |  functionPattern = 'simple-build'
               |}
               |analytics {
               |   kafka {
               |     test {
               |        bootstrapServers = '$kafkaServers'
               |     }
               |  }
               |}
               |mainClassName = "streams.TestClass"
               |
               |""".stripMargin())

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'tasks', '--all', 'showConfiguration')
              .withPluginClasspath()
              .build()

      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }

      log.warn result.getOutput()
   }

   def "All tasks run and in the correct order"() {

      given:
      ":tasks execution is successful"

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":tasks").outcome.toString())
   }

   @Unroll
   def "Executing :tasks contains :#task"() {

      when:
      "Gradle build runs"

      then:
      result.output.contains(task)

      where:
      task << ['build', 'pipelineScript', 'pipelineZip', 'publish', 'listTopics']
   }
}
