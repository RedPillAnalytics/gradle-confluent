def options = '-Si'
def properties = "-Panalytics.buildId=${env.BUILD_TAG} -g .gradle"
def gradle = "gradle ${options} ${properties}"

pipeline {
  agent {
    kubernetes {
      defaultContainer 'gradle'
      yamlFile 'pod-template.yaml'
    }
  }
  stages {
    stage('Release') {
      when {
        branch 'master'
      }
      steps {
        // create a new release
        sh "$gradle ${options} clean release -Prelease.disableChecks -Prelease.localOnly"
      }
    }
    // stage('Data Generation') {
    //   steps {
    //     container('datagen') {
    //       sh "./gradlew $options $properties generateData -PkafkaServers=broker:9092 -PpipelineEndpoint=http://ksqldb-server:8088"
    //     }
    //   }
    // }
    stage('Test') {
      steps {
        sh "$gradle cV cleanJunit build"
      }
      post {
        always {
            junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true, allowEmptyArchive: true
        }
      }
    }
  }
}
