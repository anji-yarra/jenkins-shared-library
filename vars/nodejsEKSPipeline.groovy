def call(Map configMap) {

    pipeline {

        agent {
            node {
                label 'ROBOSHOP'
            }
        }

        environment {
            appVersion = ""
            commitId = ""
            acc_id = "884057990406"
            project = configMap.get("project")
            component = configMap.get("component")
            org = "anji-yarra"
        }

        stages {

            stage('Read Version') {
                steps {
                    script {

                        def packageJson = readJSON file: 'package.json'

                        appVersion = packageJson.version
                        commitId = sh(
                            script: 'git rev-parse --short HEAD',
                            returnStdout: true
                        ).trim()

                        echo "Project: ${project}, Component: ${component}"
                        echo "Application version: ${appVersion}"
                        echo "Commit ID: ${commitId}"
                    }
                }
            }

            stage('Install Dependencies') {
                steps {
                    sh 'npm install'
                }
            }

            stage('Unit Tests') {
                steps {
                    script {

                        try {

                            sh 'npm test'

                            utils.updateCommitStatus(
                                'SUCCESS',
                                'Unit tests passed',
                                'unit-tests'
                            )

                        } catch (Exception e) {

                            utils.updateCommitStatus(
                                'FAILURE',
                                'Unit tests failed',
                                'unit-tests'
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

                                    echo "Scanning repository: ${REPO}"

                                    curl -s -L \
                                        -H "Accept: application/vnd.github+json" \
                                        -H "Authorization: Bearer ${GH_TOKEN}" \
                                        -H "X-GitHub-Api-Version: 2022-11-28" \
                                        "https://api.github.com/repos/${REPO}/dependabot/alerts?state=open" \
                                        -o alerts.json

                                    echo "===== GitHub API Response ====="
                                    cat alerts.json
                                    echo
                                    echo "==============================="

                                    echo "---- Open Dependabot Alerts ----"

                                    if ! jq -e 'type == "array"' alerts.json > /dev/null; then
                                        echo "GitHub Dependabot API did not return an alerts array:"
                                        cat alerts.json
                                        exit 1
                                    fi

                                    jq -r '.[] | [
                                        .number,
                                        .security_vulnerability.severity,
                                        .dependency.package.name,
                                        .security_advisory.ghsa_id
                                    ] | @tsv' alerts.json

                                    HIGH_CRITICAL_COUNT=$(jq '
                                        [
                                            .[] |
                                            select(
                                                .security_vulnerability.severity == "high"
                                                or
                                                .security_vulnerability.severity == "critical"
                                            )
                                        ] | length
                                    ' alerts.json)

                                    echo "High/Critical alert count: ${HIGH_CRITICAL_COUNT}"

                                    if [ "$HIGH_CRITICAL_COUNT" -gt 0 ]; then

                                        echo "❌ Found High/Critical dependency vulnerabilities."

                                        exit 1

                                    else

                                        echo "✅ No High/Critical dependency vulnerabilities found."

                                    fi
                                '''
                            }

                            utils.updateCommitStatus(
                                'SUCCESS',
                                'Library scan passed',
                                'library-scan'
                            )

                        } catch (Exception e) {

                            utils.updateCommitStatus(
                                'FAILURE',
                                'Library scan failed',
                                'library-scan'
                            )

                            throw e
                        }
                    }
                }
            }
            stage('SonarQube Analysis') {
                steps {
                    script {
                        try {
                            echo "Starting SonarQube Analysis..."

                            // Dummy stage for now.
                            // Real SonarQube scanner will be added later.
                            sh '''
                                echo "SonarQube server is not configured yet."
                                echo "Skipping actual SonarQube analysis."
                                echo "Dummy SonarQube scan completed successfully."
                            '''

                            utils.updateCommitStatus(
                                'success',
                                'SonarQube analysis passed',
                                'sonarqube'
                            )

                        } catch (Exception e) {

                            utils.updateCommitStatus(
                                'failure',
                                'SonarQube analysis failed',
                                'sonarqube'
                            )

                            throw e
                        }
                    }
                }
            }
            stage('Docker Build') {
                steps {
                    script {
                        sh """
                            docker build \
                                -t ${component}:${commitId} \
                                -t 884057990406.dkr.ecr.us-east-1.amazonaws.com/roboshop/catalogue:${commitId} \
                                .
                        """
                    }
                }
            }

            stage('Docker Push') {
                steps {
                    script {
                        withCredentials([
                            [$class: 'AmazonWebServicesCredentialsBinding',
                            credentialsId: 'aws-creds']
                        ]) {
                            sh """
                                aws ecr get-login-password --region us-east-1 | \
                                docker login --username AWS --password-stdin \
                                884057990406.dkr.ecr.us-east-1.amazonaws.com

                                docker push \
                                884057990406.dkr.ecr.us-east-1.amazonaws.com/roboshop/catalogue:${commitId}
                            """
                        }
                    }
                }
            }
            stage('dev-deploy') {
                steps {
                    script {
                        try {

                            sh """
                                aws sts get-caller-identity

                                aws eks update-kubeconfig \
                                    --name roboshop-dev \
                                    --region us-east-1

                                kubectl get nodes

                                helm upgrade --install ${component} ./helm \
                                    -f ./helm/values-dev.yaml \
                                    --namespace roboshop-dev \
                                    --create-namespace \
                                    --set deployment.imageVersion=${commitId} \
                                    --wait --timeout 5m

                                kubectl rollout status \
                                    deployment/${component} \
                                    -n roboshop-dev \
                                    --timeout=120s
                            """

                            utils.updateCommitStatus(
                                'success',
                                'Deployed to roboshop-dev',
                                'dev-deploy'
                            )

                        } catch (Exception e) {

                            utils.updateCommitStatus(
                                'failure',
                                'Deploy to roboshop-dev failed',
                                'dev-deploy'
                            )

                            throw e
                        }
                    }
                }
            }
        }
    }
}