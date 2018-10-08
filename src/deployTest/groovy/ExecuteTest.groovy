import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Slf4j
@Title("Execute :tasks")
class ProjectTest extends Specification {

   @Shared
   File projectDir, buildDir, buildFile, resourcesDir

   @Shared
   String taskName

   @Shared
   def result, taskList

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/project-test")
      buildDir = new File(projectDir, 'build')
      buildFile = new File(projectDir, 'build.gradle')
      taskList = ['exportProjectFolder']

      resourcesDir = new File('src/test/resources')

      buildFile.write("""
            plugins {
                id 'com.redpillanalytics.checkmate.odi'
            }
            
            odi {
               masterUrl = "jdbc:oracle:thin:@odi-repo.csagf46svk9g.us-east-2.rds.amazonaws.com:1521/ORCL"
               masterPassword = 'Welcome1'
               odiPassword = 'Welcome1'
            }
        """)
   }

   def setup() {

      projectDir.delete()

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

   def "Execute :createProject task"() {

      given:
      taskName = 'createProject'
      result = executeSingleTask(taskName, ['clean', '-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :exportProject task"() {

      given:
      taskName = 'exportProject'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :exportAllProjects task"() {

      given:
      taskName = 'exportAllProjects'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :exportProjectFolder task"() {

      given:
      taskName = 'exportProjectFolder'
      result = executeSingleTask(taskName, ['--folder-name=TEST_FOLDER', '-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :importAllXml task"() {

      given:
      taskName = 'importAllXML'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :importProject task with default values"() {

      given:
      taskName = 'importProject'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :importProject task with --import-path value"() {

      given:
      taskName = 'importProject'
      result = executeSingleTask(taskName, ['--import-path=src/main/odi/project-test.xml', '-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :deleteProject task"() {

      given:
      taskName = 'deleteProject'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :getProjects task"() {

      given:
      taskName = 'getProjects'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }
}
