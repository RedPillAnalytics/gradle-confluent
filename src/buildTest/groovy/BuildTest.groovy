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
   File projectDir, buildDir, resourcesDir, buildFile, settingsFile, pipelineArtifact, script

   @Shared
   String projectName = 'simple-build'

   @Shared
   def result

   def setupSpec() {

      projectDir = new File("${System.getProperty("projectDir")}/$projectName")
      buildDir = new File(projectDir, 'build')
      pipelineArtifact = new File(buildDir, 'distributions/simple-build-pipeline-1.0.0.zip')
      script = new File(buildDir, 'pipeline/ksql-script.sql')

      resourcesDir = new File('src/test/resources')

      new AntBuilder().copy(todir: projectDir) {
         fileset(dir: resourcesDir)
      }

      settingsFile = new File(projectDir, 'settings.gradle').write("""
         |rootProject.name = '$projectName'
      """.stripMargin())

      buildFile = new File(projectDir, 'build.gradle').write("""
            plugins {
               id 'com.redpillanalytics.gradle-confluent'
               id 'maven-publish'
               id 'application'
               id 'groovy'
            }
            dependencies {
               compile localGroovy()
               compile group: 'org.slf4j', name: 'slf4j-simple', version: '+'
            }

            publishing {
               publications {
                  groovy(MavenPublication) {
                     from components.java
                  }
               }
              repositories {
                mavenLocal()
                maven {
                  name 'test'
                  url 'gcs://maven.redpillanalytics.io/demo'
                }
              }
            }
            group = 'com.redpillanalytics'
            version = '1.0.0'
            
            repositories {
               jcenter()
               mavenLocal()
               maven {
               name 'test'
               url 'gcs://maven.redpillanalytics.com/demo/maven2'
              }
            }
            mainClassName = "streams.TestClass"
        """)

      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments('-Si', 'build', 'publish')
              .withPluginClasspath()
              .build()

      log.warn result.getOutput()
   }

   def "Verify the correctness of artifacts"() {

      when: 1==1

      then:
      pipelineArtifact.exists()
      script.exists()
      script.readLines().size() == 13
   }

   @Unroll
   def "Verify the following result: #task"() {

      when: 1==1

      then:
      !task.outcome != 'FAILURE'

      where:
      task << result.getTasks()
   }
}
