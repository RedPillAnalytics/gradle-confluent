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
    stage('Build') {
      steps {
        sh "$gradle build"
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
