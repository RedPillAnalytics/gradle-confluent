import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Slf4j
@Title("Test that a :build functions successfully")
class BuildTest extends Specification {

   @Shared
   File projectDir, buildDir, resourcesDir, buildFile, artifact

   @Shared
   def result

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/simple-build")
      buildDir = new File(projectDir, 'build')
      buildFile = new File(projectDir, 'build.gradle')
      artifact = new File(buildDir, 'distributions/test-pipeline.zip')


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
            
            mainClassName = "streams.TestClass"
        """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'clean', 'build', 'publish', '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()
   }

   @Unroll
   def "Verify the following result: #task"() {

      when:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'clean', 'build', 'publish', '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      then:
      !task.outcome != 'FAILURE'

      where:
      task << result.getTasks()
   }
}
