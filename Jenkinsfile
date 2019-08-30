def options = '-Si'
def properties = "-Panalytics.buildId=${env.BUILD_TAG}"
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
    stage('Data Generation') {
      steps {
        container('datagen') {
          sh 'cub kafka-ready -b localhost:9092 1 300'
          sh 'cub sr-ready localhost 8081 300'
          sh 'sleep 30'
          sh 'ksql-datagen bootstrap-server=localhost:29092 quickstart=clickstream_codes format=json topic=clickstream_codes maxInterval=20 iterations=100 &&'
          sh 'ksql-datagen bootstrap-server=localhost:29092 quickstart=clickstream_users format=json topic=clickstream_users maxInterval=10 iterations=1000 &&'
          sh 'ksql-datagen quickstart=clickstream format=json topic=clickstream maxInterval=100 bootstrap-server=localhost:29092'
        }
      }
    }
    stage('Test') {
      steps {
        sh "$gradle runAllTests"
        junit testResults: "build/test-results/test/*.xml", allowEmptyResults: true, keepLongStdio: true
      }
    }
  }
  post {
    always {
      archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
      sh "$gradle cleanJunit"
    }
  }
}
