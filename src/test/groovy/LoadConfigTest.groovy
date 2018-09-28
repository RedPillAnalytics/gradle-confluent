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
   File projectDir, buildDir, resourcesDir, buildFile, artifact, absoluteDir, absoluteFile, relativeFile, propertiesFile, processed

   @Shared
   def result, tasks, taskList

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/load-config")
      buildDir = new File(projectDir, 'build')
      buildFile = new File(projectDir, 'build.gradle')
      artifact = new File(buildDir, 'distributions/build-test-pipeline.zip')
      taskList = ['clean', 'assemble', 'check', 'createScripts', 'pipelineZip', 'build']
      absoluteDir = new File(System.getProperty("projectDir"))
      absoluteFile = new File(absoluteDir, 'streams.config')
      relativeFile = new File(projectDir, 'streams.config')
      propertiesFile = new File(projectDir, 'gradle.properties')
      processed = new File(buildDir,'resources/main/streams.properties')



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

   def "Task defaults work"() {

      given:

      propertiesFile.write('''
      APPLICATION_ID = prod-application
      ''')
      relativeFile.delete()
      absoluteFile.delete()

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', 'properties')
              .withPluginClasspath()
              .build()


      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":loadConfig").outcome.toString())
      result.output.contains('APPLICATION_ID: prod-application')
   }

   def "Task option 'configpath' works"() {

      given:

      relativeFile.write('''
      APPLICATION_ID = 'dev-application'
      ''')
      propertiesFile.delete()
      absoluteFile.delete()

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', '--config-path=streams.config', 'properties')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":loadConfig").outcome.toString())
      result.output.contains('APPLICATION_ID: dev-application')
   }

   def "Task option 'environment' works regardless"() {

      relativeFile.write('''
      APPLICATION_ID = 'dev-application'
      ''')
      propertiesFile.delete()
      absoluteFile.delete()

      given:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', '--config-env=production', 'properties')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":loadConfig").outcome.toString())
      result.output.contains('APPLICATION_ID: dev-application')
   }

   def "Absolute path works correctly with existing and new properties"() {

      given:
      absoluteFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      """)
      propertiesFile.write("""
      APPLICATION_ID = prod-application
      """)
      relativeFile.delete()

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', "--config-path=${absoluteFile.canonicalPath}", 'properties')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":loadConfig").outcome.toString())
      result.output.contains('APPLICATION_ID: dev-application')
      result.output.contains('TOPIC_PREFIX: dev-')
   }

   def "Configuring 'configPath' with project property"() {

      given:
      absoluteFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      """)
      propertiesFile.delete()
      relativeFile.delete()

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

   def "Configuring 'configPath' with 'configEnv' with project properties"() {

      given:
      propertiesFile.delete()
      relativeFile.delete()
      absoluteFile.write("""
      environments {
         production {
            APPLICATION_ID = 'dev-application'
            TOPIC_PREFIX = 'dev-'
          }
       }
      """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments(
              '-Si',
              'loadConfig',
              "-Pconfluent.configPath=${absoluteFile.canonicalPath}",
              "-Pconfluent.configEnv=production",
              'properties')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE'].contains(result.task(":loadConfig").outcome.toString())
      result.output.contains('APPLICATION_ID: dev-application')
      result.output.contains('TOPIC_PREFIX: dev-')
   }

   def "Configuring 'config-path' with 'config-env' with task options"() {

      given:
      propertiesFile.delete()
      relativeFile.delete()
      absoluteFile.write("""
      environments {
         production {
            APPLICATION_ID = 'dev-application'
            TOPIC_PREFIX = 'dev-'
          }
       }
      """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments(
              '-Si',
              'loadConfig',
              "--config-path=${absoluteFile.canonicalPath}",
              "--config-env=production",
              'properties')
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

      propertiesFile.delete()
      relativeFile.delete()
      absoluteFile.delete()

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'loadConfig', 'properties')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":loadConfig").outcome.toString())
      !result.output.contains('APPLICATION_ID: dev-application')
   }

   def "Application Plugin expand works"() {

      given:

      relativeFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      """)

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
   }

}
