def call (Map configMap) {
    pipeline {
        agent any
        environment {
            projectName = configMap.get("projectName")
            componentName = configMap.get("componentName")
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