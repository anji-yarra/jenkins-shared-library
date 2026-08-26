def call(Map configMap) {
    pipeline {
        agent {
            node {
                label 'ROBOSHOP'
            }
        }

        stages {
            stage('Test Shared Library') {
                steps {
                    echo "Project: ${configMap.projectName}, Component: ${configMap.componentName}"
                }
            }
        }
    }
}