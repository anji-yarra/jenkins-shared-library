def call (Map configMap) {
    pipeline {
        agent any
        environment {
            projectName = configMap.projectName
            componentName = configMap.componentName
        }
        stages {
            stage('Build') {
                steps {
                    echo "Project: ${configMap.projectName}, Component: ${configMap.componentName}"
                }
            }
            stage('Test') {
                steps {
                    echo "Project: ${configMap.projectName}, Component: ${configMap.componentName}"
                }
            }
            stage('Deploy') {
                steps {
                    echo "Project: ${configMap.projectName}, Component: ${configMap.componentName}"
                }
            }
        }
    }
}