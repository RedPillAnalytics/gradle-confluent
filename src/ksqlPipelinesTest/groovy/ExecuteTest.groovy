import groovy.ant.AntBuilder
import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.time.Duration

@Slf4j
@Stepwise
@Testcontainers
class ExecuteTest extends Specification {

   @Shared
   DockerComposeContainer environment =
           new DockerComposeContainer<>(new File('docker-compose.yml'))
                   .withExposedService("ksqldb-server", 8088, Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))
                   .withExposedService('kafka', 29092)
                   .withLocalCompose(true)
   @Shared
   File projectDir, buildDir, buildFile, settingsFile, resourcesDir

   @Shared
   String projectName = 'execute-test'

   @Shared
   String taskName, kafka, endpoint

   @Shared
   def result, taskList

   @Shared
   String analyticsVersion = System.getProperty("analyticsVersion")

   def copyResources() {
      new groovy.util.AntBuilder().copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }
   }

   def setupSpec() {
      projectDir = new File("${System.getProperty("projectDir")}/$projectName")
      buildDir = new File(projectDir, 'build')
      taskList = ['pipelineExecute']
      resourcesDir = new File('src/test/resources')
      buildFile = new File(projectDir, 'build.gradle')
      endpoint = "http://${environment.getServiceHost('ksqldb-server', 8088)}:${environment.getServicePort('ksqldb-server', 8088)}".toString()
      kafka = "${environment.getServiceHost('kafka', 29092)}:${environment.getServicePort('kafka', 29092)}".toString()

      copyResources()

      settingsFile = new File(projectDir, 'settings.gradle').write("""rootProject.name = '$projectName'""")
      buildFile.write("""
               |plugins {
               |  id 'com.redpillanalytics.gradle-confluent'
               |  id "com.redpillanalytics.gradle-analytics" version "$analyticsVersion"
               |}
               |
               |confluent {
               |  pipelineEndpoint '$endpoint'
               |}
               |
               |analytics {
               |   kafka {
               |     test {
               |        bootstrapServers = '$kafka'
               |     }
               |  }
               |}
               |""".stripMargin())
   }

   def setup() {
      copyResources()
   }

   // helper method
   def executeSingleTask(String taskName, List otherArgs, Boolean logOutput = true) {

      otherArgs.add(0, taskName)
      log.warn "runner arguments: ${otherArgs.toString()}"

      // execute the Gradle test build
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments(otherArgs)
              .withPluginClasspath()
              .build()

      // log the results
      if (logOutput) log.warn result.getOutput()

      return result
   }

   def "Execute :tasks task"() {
      given:
      taskName = 'tasks'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }


   def "Execute :listTopics task"() {
      given:
      taskName = 'listTopics'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }

   def "Execute :pipelineExecute task with default values"() {
      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }

   def "Execute :pipelineExecute task with --drop-only first"() {

      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--drop-only', '-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
      !result.output.toLowerCase().contains('create table')
      !result.output.toLowerCase().contains('insert into')
   }

   def "Execute :pipelineExecute task with custom directory"() {
      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--pipeline-dir=src/main/pipeline/01-clickstream', '-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }

   def "Execute :pipelineExecute task with --drop-only second"() {
      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--drop-only', '-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
      !result.output.toLowerCase().contains('create table')
      !result.output.toLowerCase().contains('insert into')
   }

   def "Execute :pipelineExecute task with --no-drop"() {
      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--no-drop', '-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
      !result.output.toLowerCase().contains('drop table')
   }

   def "Execute :pipelineExecute task with --drop-only third"() {
      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--drop-only', '-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
      !result.output.toLowerCase().contains('create table')
      !result.output.toLowerCase().contains('insert into')
   }

   def "Execute :pipelineExecute task with --no-terminate"() {
      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--no-terminate', '-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
      !result.output.toLowerCase().contains('terminating query')
   }

   def "Execute :pipelineExecute task with --from-beginning"() {
      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--from-beginning', '-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }

   def "Execute :pipelineExecute and test for --@DeleteTopic directive"() {
      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--drop-only', '-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'
      result.output.toLowerCase().contains('drop table if exists events_per_min delete topic')
   }

   def "Execute :pipelineExecute task with custom REST endpoint"() {
      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ["--rest-url", endpoint, '-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'
   }

   def "Execute :producer task"() {
      given:
      taskName = 'producer'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'
      result.tasks.collect { it.path - ":" } == ['kafkaTestSink', 'producer']
   }
}
