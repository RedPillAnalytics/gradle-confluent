import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Slf4j
@Title("Execute :properties task")
class PropertiesTest extends Specification {
   @Shared
   File projectDir, buildDir, buildFile, resourcesDir, settingsFile

   @Shared
   String taskName

   @Shared
   List tasks

   @Shared
   BuildResult result

   @Shared
   String projectName = 'run-properties'

   @Shared
   AntBuilder ant = new AntBuilder()

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/$projectName")
      buildDir = new File(projectDir, 'build')
      buildFile = new File(projectDir, 'build.gradle')

      resourcesDir = new File('src/test/resources')

      ant.copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }

      buildFile.write("""
               |plugins {
               |  id 'com.redpillanalytics.gradle-confluent'
               |  id "com.redpillanalytics.gradle-analytics" version "1.2.1"
               |  id 'maven-publish'
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
               |     url 's3://maven.redpillanalytics.com/demo/maven2'
               |     authentication {
               |        awsIm(AwsImAuthentication)
               |     }
               |  }
               |}
               |
               |dependencies {
               |   archives group: 'com.redpillanalytics', name: 'simple-build', version: '+'
               |   archives group: 'com.redpillanalytics', name: 'simple-build-pipeline', version: '+'
               |}
               |
               |analytics.sinks {
               |   kafka
               |}
               |
               |""".stripMargin())

      settingsFile = new File(projectDir, 'settings.gradle').write("""rootProject.name = '$projectName'""")

   }

   def executeSingleTask(String taskName, List otherArgs) {

      otherArgs.add(0, taskName)

      log.warn "runner arguments: ${otherArgs.toString()}"

      // execute the Gradle test build
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments(otherArgs)
              .withPluginClasspath()
              .forwardOutput()
              .build()
   }

   def "Execute :properties with enableStreams = true"() {

      given:
      taskName = 'properties'
      result = executeSingleTask(taskName, ['-Si','-Pconfluent.enableStreams=true',"-PmainClassName=streams.TestClass"])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }

   def "Execute :properties with enableFunctions = true"() {

      given:
      taskName = 'properties'
      result = executeSingleTask(taskName, ['-Si','-Pconfluent.enableFunctions=true','-Pconfluent.functionPattern = simple-build'])

      expect:
      result.task(":${taskName}").outcome.name() != 'FAILED'

   }
}
