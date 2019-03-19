import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Title

@Slf4j
@Stepwise
@Title("Execute :tasks")
class ExecuteTest extends Specification {

   @Shared
   File projectDir, buildDir, buildFile, settingsFile, resourcesDir

   @Shared
   String projectName = 'execute-test'

   @Shared
   String taskName

   @Shared
   def result, taskList

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/$projectName")
      buildDir = new File(projectDir, 'build')
      taskList = ['pipelineExecute']

      resourcesDir = new File('src/test/resources')

      copySource()

      settingsFile = new File(projectDir, 'settings.gradle').write("""rootProject.name = '$projectName'""")

      buildFile = new File(projectDir, 'build.gradle')

      buildFile.write("""
               |plugins {
               |  id 'com.redpillanalytics.gradle-confluent'
               |  id "com.redpillanalytics.gradle-analytics" version "1.1.23"
               |}
               |confluent.pipelineEndpoint = 'http://localhost:8088'
               |""".stripMargin())
   }

   def setup() {

      copySource()
   }

   def copySource() {

      new AntBuilder().copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }
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

   def "Execute :listTopics task"() {

      given:
      taskName = 'listTopics'
      result = executeSingleTask(taskName, ['-Si', '--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :pipelineExecute task with default values"() {

      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['-Si', '--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :pipelineExecute task with custom directory"() {

      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--pipeline-dir=src/main/pipeline/01-clickstream', '-Si', '--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :pipelineExecute task with --no-create"() {

      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--no-create', '-Si', '--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'
      !result.output.toLowerCase().contains('create table')
      !result.output.toLowerCase().contains('insert into')

   }

   def "Execute :pipelineExecute task with --no-drop"() {

      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--no-drop', '-Si', '--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'
      !result.output.toLowerCase().contains('drop table')

   }

   def "Execute :pipelineExecute task with --no-create again"() {

      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--no-create', '-Si', '--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'
      !result.output.toLowerCase().contains('create table')
      !result.output.toLowerCase().contains('insert into')

   }

   def "Execute :pipelineExecute task with --no-terminate"() {

      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--no-terminate', '-Si', '--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'
      !result.output.toLowerCase().contains('terminating query')

   }

   def "Execute :pipelineExecute task with --from-beginning"() {

      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--from-beginning', '-Si', '--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :pipelineExecute and test for --@DeleteTopic directive"() {

      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['--no-create', '-Si', '--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'
      result.output.toLowerCase().contains('drop table if exists events_per_min delete topic')

   }

   def "Execute :pipelineExecute task with custom REST endpoint"() {

      given:
      buildFile.write("""
               |plugins {
               |  id 'com.redpillanalytics.gradle-confluent'
               |}
               confluent.pipelineEndpoint = 'http://nothing:8088'
        """.stripMargin())

      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ["-Pconfluent.pipelineEndpoint=http://localhost:8088", '-Si', '--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }
}
