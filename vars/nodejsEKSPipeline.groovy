def call(Map configMap) {
    pipeline {
        agent {
            node {
                label 'ROBOSHOP'
            }
        }
        environment {
            def appVersion = ""
            acc_id = "160885265516"
            project = configMap.get("project")
            component = configMap.get("component")
            org = "anji-yarra"
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
            stage('Unit tests') {
                steps {
                    script {
                        try {
                            sh 'npm test'

                            utils.updateCommitStatus(
                                'SUCCESS',
                                'Unit tests passed',
                                'unit-tests',
                            )
                        } catch (Exception e) {
                            utils.updateCommitStatus(
                                'FAILURE',
                                'Unit tests failed',
                                'unit-tests',
                            )
                            throw e
                        }
                    }
                }
            }
            stage('Library Scan') {
                steps {
                    script {
                        try {
                            withCredentials([
                                string(
                                    credentialsId: 'github-token',
                                    variable: 'GH_TOKEN'
                                )
                            ]) {
                                sh '''
                                    set -e

                                    REPO="${org}/${component}"

                                    curl -s -L \
                                        -H "Accept: application/vnd.github+json" \
                                        -H "Authorization: Bearer ${GH_TOKEN}" \
                                        -H "X-GitHub-Api-Version: 2022-11-28" \
                                        "https://api.github.com/repos/${REPO}/dependabot/alerts?state=open" \
                                        -o alerts.json

                                    echo "---- Open Dependabot Alerts ----"

                                    jq -r '.[] |
                                        "\(.number)\t\(.security_vulnerability.severity)\t\(.dependency.package.name)\t\(.security_advisory.ghsa_id)"' \
                                        alerts.json

                                    HIGH_CRITICAL_COUNT=$(jq '
                                        [.[] |
                                        select(
                                            .security_vulnerability.severity == "high"
                                            or
                                            .security_vulnerability.severity == "critical"
                                        )] | length
                                    ' alerts.json)

                                    echo "High/Critical alert count: ${HIGH_CRITICAL_COUNT}"

                                    if [ "$HIGH_CRITICAL_COUNT" -gt 0 ]; then
                                        echo "Found High/Critical dependency vulnerabilities."
                                        exit 1
                                    else
                                        echo "No High/Critical dependency vulnerabilities found."
                                    fi
                                '''
                            }

                            utils.updateCommitStatus(
                                'success',
                                'Library scan passed',
                                'library-scan'
                            )

                        } catch (Exception e) {

                            utils.updateCommitStatus(
                                'failure',
                                'Library scan failed',
                                'library-scan'
                            )

                            throw e
                        }
                    }
                }
            }
        }
    }
}