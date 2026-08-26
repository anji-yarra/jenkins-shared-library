def call ( ) {
    pipeline {
        agent any
    
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