def options = '-S'
def properties = "-Panalytics.buildTag=${env.BUILD_TAG}"
def gradle = "./gradlew ${options} ${properties}"

pipeline {
   agent {
      kubernetes {
         defaultContainer 'agent'
         yamlFile 'pod-template.yaml'
         slaveConnectTimeout 200
      }
   }
   environment {
      ORG_GRADLE_PROJECT_githubToken = credentials('github-redpillanalyticsbot-secret')
      ORG_GRADLE_PROJECT_kafkaServers = 'localhost:9092'
      AWS = credentials("rpa-development-build-server-svc")
      AWS_ACCESS_KEY_ID = "${env.AWS_USR}"
      AWS_SECRET_ACCESS_KEY = "${env.AWS_PSW}"
      AWS_REGION = 'us-east-1'
      GRADLE_COMBINED = credentials("gradle-publish-key")
      GRADLE_KEY = "${env.GRADLE_COMBINED_USR}"
      GRADLE_SECRET = "${env.GRADLE_COMBINED_PSW}"
   }

   stages {

      stage('Release') {
         when { branch "master" }
         steps {
            sh "$gradle clean release -Prelease.disableChecks -Prelease.localOnly"
         }
      }

      stage('Test') {
         steps {
            sh "$gradle cleanJunit cV runAllTests"
         }
         post {
            always {
               junit testResults: 'build/test-results/**/*.xml', allowEmptyResults: true
            }
         }
      }

      stage('Publish') {
         when { branch "master" }
         steps {
            sh "$gradle publish -Pgradle.publish.key=${env.GRADLE_KEY} -Pgradle.publish.secret=${env.GRADLE_SECRET}"
         }
         post {
            always {
               archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true, allowEmptyArchive: true
            }
         }
      }
   }
}
