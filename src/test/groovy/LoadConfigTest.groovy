import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Slf4j
@Title("Check basic configuration")
class LoadConfigTest extends Specification {

   @Shared
   File projectDir, buildDir, pipelineDir, pipelineScript, resourcesDir, buildFile, settingsFile, artifact, absoluteDir, absoluteFile, relativeFile, processed, unixScript, windowsScript

   @Shared
   def result, taskList

   @Shared
   String projectName = 'load-config'

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/$projectName")
      buildDir = new File(projectDir, 'build')
      pipelineDir = new File(buildDir, 'pipeline')
      pipelineScript = new File(pipelineDir, 'ksql-script.sql')
      artifact = new File(buildDir, 'distributions/build-test-pipeline.zip')
      taskList = ['clean', 'assemble', 'check', 'pipelineScript', 'pipelineZip', 'build']
      absoluteDir = new File(System.getProperty("projectDir"))
      absoluteFile = new File(absoluteDir, 'streams.config')
      relativeFile = new File(projectDir, 'streams.config')
      processed = new File(buildDir, 'resources/main/streams.properties')
      unixScript = new File(buildDir, 'scripts/load-config')
      windowsScript = new File(buildDir, 'scripts/load-config.bat')

      resourcesDir = new File('src/test/resources')

      new AntBuilder().copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }

      settingsFile = new File(projectDir, 'settings.gradle').write("""rootProject.name = '$projectName'""")

      buildFile = new File(projectDir, 'build.gradle').write("""
            plugins {
               id 'com.redpillanalytics.gradle-confluent'
               id 'maven-publish'
               id 'application'
            }

            archivesBaseName = 'test'
            group = 'com.redpillanalytics'
            version = '1.0.0'
            
            mainClassName = "streams.TestClass"
        """)
   }

   def setup() {

      relativeFile.delete()
      absoluteFile.delete()
      processed.delete()
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
              .withArguments('-Si', 'build', "-P.confluent.configPath=streams.config", '--rerun-tasks')
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
              .withArguments('-Si', 'build', "-Pconfluent.configPath=${absoluteFile.canonicalPath}", '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":build").outcome.toString())
      processed.exists()
      processed.text.contains('APPLICATION_ID = dev-application')
      processed.text.contains('TOPIC_PREFIX = dev-')
   }

   def "Application Plugin expand works with absolute file and environment"() {

      given:

      absoluteFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      
      environments {
        test {
           APPLICATION_ID = 'test-application'
           TOPIC_PREFIX = 'test-'
         }
       }
      """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'build', "-Pconfluent.configPath=${absoluteFile.canonicalPath}", "-Pconfluent.configEnv=test", '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":build").outcome.toString())
      processed.exists()
      processed.text.contains('APPLICATION_ID = test-application')
      processed.text.contains('TOPIC_PREFIX = test-')
   }

   def "Application Plugin expand works with absolute file and bogus environment"() {

      given:

      absoluteFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      
      environments {
        test {
           APPLICATION_ID = 'test-application'
           TOPIC_PREFIX = 'test-'
         }
       }
      """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'build', "-Pconfluent.configPath=${absoluteFile.canonicalPath}", "-Pconfluent.configEnv=nothing", '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":build").outcome.toString())
      processed.exists()
      processed.text.contains('APPLICATION_ID = dev-application')
      processed.text.contains('TOPIC_PREFIX = dev-')
   }

   def "Application Plugin applicationDefaultJvmArgs are replaced"() {

      given:

      relativeFile.write("""
      APPLICATION_ID = 'dev-application'
      TOPIC_PREFIX = 'dev-'
      applicationDefaultJvmArgs = '-Djava.io.tmpdir=/tmp'
      """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'build', '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      expect:
      ['SUCCESS', 'UP_TO_DATE', 'SKIPPED'].contains(result.task(":build").outcome.toString())
      unixScript.exists()
      unixScript.text.contains('''DEFAULT_JVM_OPTS="-Djava.io.tmpdir=/tmp"''')
      windowsScript.exists()
      windowsScript.text.contains('''set DEFAULT_JVM_OPTS="-Djava.io.tmpdir=/tmp"''')
   }
}
