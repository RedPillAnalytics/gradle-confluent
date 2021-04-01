import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Title

@Slf4j
@Stepwise
@Title("Check basic configuration")
class DeployTest extends Specification {

   @Shared
   File projectDir, buildDir, resourcesDir, settingsFile, artifact, buildFile

   @Shared
   def result, tasks

   @Shared
   String projectName = 'simple-deploy', taskName

   @Shared
   String pipelineEndpoint = System.getProperty("pipelineEndpoint") ?: 'http://localhost:8088'

   @Shared
   String kafkaServers = System.getProperty("kafkaServers") ?: 'localhost:9092'

   @Shared
   String analyticsVersion = System.getProperty("analyticsVersion")

   def setup() {

      copySource()
   }

   def copySource() {

      new AntBuilder().copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }
   }

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/$projectName")
      buildDir = new File(projectDir, 'build')
      artifact = new File(buildDir, 'distributions/simple-deploy-pipeline.zip')

      resourcesDir = new File('src/test/resources')

      copySource()

      settingsFile = new File(projectDir, 'settings.gradle').write("""rootProject.name = '$projectName'""")

      buildFile = new File(projectDir, 'build.gradle')

      buildFile.write("""
               |plugins {
               |  id 'com.redpillanalytics.gradle-confluent'
               |  id "com.redpillanalytics.gradle-analytics" version "$analyticsVersion"
               |  id 'maven-publish'
               |}
               |
               |publishing {
               |  repositories {
               |    mavenLocal()
               |  }
               |}
               |group = 'com.redpillanalytics'
               |version = '1.0.0'
               |
               |repositories {
               |  jcenter()
               |  mavenLocal()
               |  maven {
               |     name 'test'
               |     url 'maven'
               |  }
               |}
               |
               |dependencies {
               |   archives group: 'com.redpillanalytics', name: 'simple-build', version: '+'
               |   archives group: 'com.redpillanalytics', name: 'simple-build-pipeline', version: '+'
               |}
               |
               |confluent {
               |  functionPattern = 'simple-build'
               |  pipelineEndpoint = '$pipelineEndpoint'
               |}
               |analytics {
               |   kafka {
               |     test {
               |        bootstrapServers = '$kafkaServers'
               |     }
               |  }
               |}
               |""".stripMargin())
   }

   // helper method
   def executeSingleTask(String taskName, List otherArgs = []) {

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

   def "Execute :tasks task"() {
      given:
      taskName = 'tasks'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILED')
   }

   def "Deploy test from Resources"() {
      given:
      taskName = 'deploy'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILED')
      result.tasks.collect { it.path - ":" } == ["functionCopy", "pipelineExtract", "pipelineDeploy", "deploy"]
   }

   def "Producer test to Kafka"() {
      given:
      taskName = 'producer'
      result = executeSingleTask(taskName, ['-Si'])

      expect:
      !result.tasks.collect { it.outcome }.contains('FAILED')
      result.tasks.collect { it.path - ":" } == ['kafkaTestSink', 'producer']
   }
}
