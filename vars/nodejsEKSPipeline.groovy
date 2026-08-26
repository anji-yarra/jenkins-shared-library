def call(Map configMap) {
    pipeline {
        agent {
            node {
                label 'ROBOSHOP'
            }
        }

        stages {
            stage('Read Version') {
                steps {
                    script {
                        def packageJson = readJSON file: 'package.json'
                        def appVersion = packageJson.version

                        echo "Project: ${configMap.projectName}, Component: ${configMap.componentName}"
                        echo "The application version is: ${appVersion}"
                    }
                }
            }
            stage('Install Dependencies') {
                steps {
                    script {
                        sh """
                            npm install
                        """
                    }
                }
            }
        }
    }
}