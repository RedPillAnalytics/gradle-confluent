def options = '-Si'
def properties = "-Panalytics.buildId=${env.BUILD_TAG}"
def gradle = "./gradlew ${options} ${properties}"

pipeline {
   agent { label 'java-compile' }

   environment {
      GOOGLE_APPLICATION_CREDENTIALS = '/var/lib/jenkins/.gcp/gradle-analytics-build-user.json'
   }

   stages {

      stage('Release') {
         when { branch "master" }
         steps {
            sh "$gradle ${options} clean release -Prelease.disableChecks -Prelease.localOnly"
         }
      }

      stage('Build') {
         steps {
            sh "$gradle build"
            junit testResults: "build/test-results/**/*.xml", allowEmptyResults: true
         }
      }

      stage('Integration') {
          steps {
              sh "./bin/confluent start"
              sh "$gradle ksqlServertest"
              junit testResults: "build/test-results/**/*.xml", allowEmptyResults: true
          }
      }

      stage('Publish') {
         when { branch "master" }
         steps {
            sh "$gradle ${options} publishPlugins"
         }
      }
      // Place for new Stage

   } // end of Stages

   post {
      always {
         archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
         //sh "$gradle producer"
      }
   }

}
