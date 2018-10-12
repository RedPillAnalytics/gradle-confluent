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
   File projectDir, buildDir, resourcesDir, buildFile, pipelineArtifact, pipelineCreate, pipelineDrop

   @Shared
   def result

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/simple-build")
      buildDir = new File(projectDir, 'build')
      buildFile = new File(projectDir, 'build.gradle')
      pipelineArtifact = new File(buildDir, 'distributions/simple-build-pipeline-1.0.0.zip')
      pipelineCreate = new File(buildDir, 'pipeline-build/ksql-create-script.sql')
      pipelineDrop = new File(buildDir, 'pipeline-build/ksql-drop-script.sql')


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

   def "Verify the correctness of artifacts"() {

      when:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'build', 'publish')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      then:
      pipelineArtifact.exists()
      pipelineCreate.exists()
      pipelineDrop.exists()
      pipelineCreate.readLines().size() == 14
      pipelineDrop.readLines().size() == 14
   }

   @Unroll
   def "Verify the following result: #task"() {

      when:
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'clean', 'build', '--rerun-tasks')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()

      then:
      !task.outcome != 'FAILURE'

      where:
      task << result.getTasks()
   }
}
