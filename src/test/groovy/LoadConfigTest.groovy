import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Slf4j
@Title("Check basic configuration")
class LoadConfigTest extends Specification {

   @Shared
   File projectDir, buildDir, resourcesDir, buildFile, artifact, absoluteDir, absoluteFile, relativeFile, processed

   @Shared
   def result, taskList

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/load-config")
      buildDir = new File(projectDir, 'build')
      buildFile = new File(projectDir, 'build.gradle')
      artifact = new File(buildDir, 'distributions/build-test-pipeline.zip')
      taskList = ['clean', 'assemble', 'check', 'createScripts', 'pipelineZip', 'build']
      absoluteDir = new File(System.getProperty("projectDir"))
      absoluteFile = new File(absoluteDir, 'streams.config')
      relativeFile = new File(projectDir, 'streams.config')
      processed = new File(buildDir, 'resources/main/streams.properties')

      resourcesDir = new File('src/test/resources')

      new AntBuilder().copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }

      buildFile.write("""
            plugins {
               id 'com.redpillanalytics.gradle-confluent'
               id 'maven-publish'
               id 'application'
            }

            archivesBaseName = 'test'
            group = 'com.redpillanalytics'
            version = '1.0.0'
            
            dependencies {

               //standard groovy
               compile group: 'org.codehaus.groovy', name: 'groovy-all', version: '2.4.15'
               compile group: 'org.slf4j', name: 'slf4j-simple', version: '+'
            
            }
            
            repositories {
               jcenter()
            }
            
            mainClassName = "streams.TestClass"
        """)
   }

   def setup() {

      relativeFile.delete()
      absoluteFile.delete()
   }

   def "Application Plugin expand works with default file"() {

      given:

      relativeFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'build', '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":build").outcome.toString())
      processed.exists()
      processed.text.contains('APPLICATION_ID = dev-application')
      processed.text.contains('TOPIC_PREFIX = dev-')
   }

   def "Application Plugin expand works with relative file"() {

      given:

      relativeFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'build', "-PconfigPath=streams.config", '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":build").outcome.toString())
      processed.exists()
      processed.text.contains('APPLICATION_ID = dev-application')
      processed.text.contains('TOPIC_PREFIX = dev-')
   }

   def "Application Plugin expand works with absolute file"() {

      given:

      absoluteFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'build', "-PconfigPath=${absoluteFile.canonicalPath}", '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":build").outcome.toString())
      processed.exists()
      processed.text.contains('APPLICATION_ID = dev-application')
      processed.text.contains('TOPIC_PREFIX = dev-')
   }

}
