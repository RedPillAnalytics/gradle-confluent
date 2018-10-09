import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Slf4j
@Title("Execute :tasks")
class ExecuteTest extends Specification {

   @Shared
   File projectDir, buildDir, buildFile, resourcesDir

   @Shared
   String taskName

   @Shared
   def result, taskList

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/execute-test")
      buildDir = new File(projectDir, 'build')
      buildFile = new File(projectDir, 'build.gradle')
      taskList = ['pipelineExecute']

      resourcesDir = new File('src/test/resources')

      copySource()

      buildFile.write("""
               |plugins {
               |  id 'com.redpillanalytics.gradle-confluent'
               |}
               |
               |archivesBaseName = 'test'
               |group = 'com.redpillanalytics'
               |version = '1.0.0'
        """.stripMargin())
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

   @Ignore
   def "Execute :pipelineExecute task with default values"() {

      given:
      taskName = 'pipelineExecute'
      result = executeSingleTask(taskName, ['-Si','--rerun-tasks'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }
}
