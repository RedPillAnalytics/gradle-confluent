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
   File projectDir, buildDir, resourcesDir, buildFile, artifact, absoluteDir, absoluteFile, relativeFile

   @Shared
   def result, tasks, taskList

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/load-config")
      buildDir = new File(projectDir, 'build')
      buildFile = new File(projectDir, 'build.gradle')
      artifact = new File(buildDir, 'distributions/build-test-pipeline.zip')
      taskList = ['clean', 'assemble', 'check', 'createScripts', 'pipelineZip', 'build']
      absoluteDir = new File(System.getProperty("projectDir"))
      absoluteFile = new File(absoluteDir,'streams.config')
      relativeFile = new File(projectDir,'streams.config')



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
        """)
   }

   def "Task option 'configpath' works"() {

      given:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', '--configpath=streams.config')
              .withPluginClasspath()
              .build()

      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":loadConfig").outcome.toString())
   }

   def "Task option 'environment' works"() {

      given:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', '--environment=production')
              .withPluginClasspath()
              .build()

      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":loadConfig").outcome.toString())
   }

   def "Task defaults work"() {

      given:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig')
              .withPluginClasspath()
              .build()

      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":loadConfig").outcome.toString())
   }

   def "Properties are overridden"() {

      given:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', 'properties')
              .withPluginClasspath()
              .build()

      tasks = result.output.readLines().grep(~/(> Task :)(.+)/).collect {
         it.replaceAll(/(> Task :)(\w+)( UP-TO-DATE)*/, '$2')
      }

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":loadConfig").outcome.toString())
      result.output.contains('APPLICATION_ID: prod-application')
   }

   def "Absolute path works correctly with existing and new properties"() {

      given:
      absoluteFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', "--configpath=${absoluteFile.canonicalPath}", 'properties')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":loadConfig").outcome.toString())
      result.output.contains('APPLICATION_ID: dev-application')
      result.output.contains('TOPIC_PREFIX: dev-')
   }

   def "Project property works for configuring 'configpath'"() {

      given:
      absoluteFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', "-Pconfluent.configPath=${absoluteFile.canonicalPath}", 'properties')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":loadConfig").outcome.toString())
      result.output.contains('APPLICATION_ID: dev-application')
      result.output.contains('TOPIC_PREFIX: dev-')
   }

   def "No failures when :loadConfig is called with no file"() {

      given:

      relativeFile.delete()

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', 'properties')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE','SKIPPED'].contains(result.task(":loadConfig").outcome.toString())
      !result.output.contains('APPLICATION_ID: dev-application')
   }

}
