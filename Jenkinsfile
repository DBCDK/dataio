#!groovy

String workerNode = "devel11-java17"

pipeline {
    agent { label workerNode }
    tools {
        maven 'maven 3.9'
    }
    environment {
        MAVEN_OPTS = "-Dmaven.repo.local=/home/isworker/.m2/dataio-repo -XX:ReservedCodeCacheSize=256m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
        ARTIFACTORY_LOGIN = credentials("artifactory_login")
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        DEPLOY_ARTIFACTS = "commons/utils/flow-store-service-connector, \
            commons/utils/tickle-harvester-service-connector, \
            harvester/framework, \
            gatekeeper, \
            harvester/utils/rawrepo-connector, \
            harvester/utils/harvester-job-builder, \
            commons/utils, \
            commons/utils/binary-file-store, \
            cli/acc-test-runner, \
            cli/flow-test-runner"
    }
    triggers {
        upstream(upstreamProjects: "Docker-payara6-bump-trigger",
                threshold: hudson.model.Result.SUCCESS)
    }
    options {
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(artifactDaysToKeepStr: "",
                artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
        timeout(time: 1, unit: "HOURS")
        disableConcurrentBuilds(abortPrevious: true)
        lock('dataio-build')
    }
    stages {
        stage('clean and checkout') {
            steps {
                cleanWs()
                sh """
                    test -d /home/isworker/.m2/dataio-repo/dk/dbc && rm -r /home/isworker/.m2/dataio-repo/dk/dbc || echo ok
                """
                checkout scm
            }
        }
        stage("build") {
            steps {
                withSonarQubeEnv(installationName: 'sonarqube.dbc.dk') {
                    script {
                        def status = sh returnStatus: true, script: """
                            mvn -B --no-transfer-progress -T 1 -Dtag="${env.BRANCH_NAME}-${env.BUILD_NUMBER}" install
                        """

                        if (status == 0) {
                            def sonarOptions = "-Dsonar.branch.name=$BRANCH_NAME"
                            if (env.BRANCH_NAME != 'master') {
                                sonarOptions += " -Dsonar.newCode.referenceBranch=master"
                            }
                            status += sh returnStatus: true, script: """
                                mvn -B --no-transfer-progress $sonarOptions sonar:sonar
                            """
                        }

                        junit allowEmptyResults: true, testResults: '**/target/*-reports/*.xml'

                        if (status != 0) {
                            error("build failed")
                        }

                        archiveArtifacts artifacts: "cli/acceptance-test/target/dataio-cli-acctest.jar,gatekeeper/target/dataio-gatekeeper*.jar,cli/dataio-cli",
                            fingerprint: true
                    }
                }
            }
        }
        stage("quality gate") {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage("deploy to mavenrepo.dbc.dk") {
            when {
                branch "master"
            }
            steps {
                sh """
                    mvn -B --no-transfer-progress -Dcyclonedx.skip=true deploy -T 1 -Dmaven.test.skip=true -Ddocker.skip=true -pl "${DEPLOY_ARTIFACTS}" -am
                """
            }
        }
        stage("publish docker images") {
            steps {
                script {
                    // The -Pdocker-push method is to be gradually phased out
                    // in favour of the docker/publish.sh script.
                    if (env.BRANCH_NAME != 'master') {
                        sh """
                            mvn -B --no-transfer-progress -Dcyclonedx.skip=true install -T 1 -Dmaven.test.skip=true -Pdocker-push -Dtag="${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
                        """
                    } else {
                        sh """
                            mvn -B --no-transfer-progress -Dcyclonedx.skip=true install -T 1 -Dmaven.test.skip=true -Pdocker-push
                        """
                    }

                    sh """
                        ./docker/publish.sh
                    """
                }
            }
        }
        stage("clean up docker images") {
            when {
                branch "master"
            }
            steps {
                sh """
                    ./docker/remove-images docker-metascrum.artifacts.dbccloud.dk/dbc-payara-*
                    ./docker/remove-images docker-metascrum.artifacts.dbccloud.dk/dataio-*
                """
            }
        }
        stage("update staging version") {
            when {
                branch "master"
            }
            steps {
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: "gitlab-isworker", keyFileVariable: 'sshkeyfile')]) {
                        env.GIT_SSH_COMMAND = "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i ${sshkeyfile}"
                        sh '''
                            nix run --refresh git+https://gitlab.dbc.dk/public-de-team/gitops-secrets-set-variables.git \
                                metascrum-staging:DATAIO_VERSION=$BUILD_NUMBER
                        '''
                    }
                }
            }
        }
        stage("update dit version") {
            agent {
                docker {
                    label workerNode
                    image "docker.dbc.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                        set-new-version services/dataio-project "${env.GITLAB_PRIVATE_TOKEN}" metascrum/dit-gitops-secrets "${env.BUILD_NUMBER}" -b master
                    """
                }
            }
        }
    }
    post {
        always {
            echo 'Cleaning up'
            cleanWs()
        }
    }
}
