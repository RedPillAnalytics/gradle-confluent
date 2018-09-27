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
         }
      }

      stage('Integration') {
          steps {
              sh "/var/lib/jenkins/confluent/confluent-5.0.0/bin/confluent start"
              sh "$gradle ksqlServertest"
          }
      }

      stage('Publish') {
         when { branch "master" }
         steps {
            sh "$gradle ${options} publishPlugins githubRelease"
         }
      }
      // Place for new Stage

   } // end of Stages

   post {
      always {
         junit testResults: "build/test-results/**/*.xml", allowEmptyResults: true, keepLongStdio: true
         archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
         //sh "$gradle producer"
      }
   }

}
