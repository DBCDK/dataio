#!groovy

def docker_images_log_stash_tag = "docker_images_log"
def workerNode = "devel11"

pipeline {
    agent {label workerNode}
    tools {
		jdk 'jdk11'
		maven 'Maven 3'
    }
    environment {
        MAVEN_OPTS="-Dmaven.repo.local=/home/isworker/.dataio_repo -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
        ARTIFACTORY_LOGIN = credentials("artifactory_login")
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
        BUILD_NUMBER="${env.BUILD_NUMBER}"
        DEPLOY_ARTIFACTS="commons/utils/flow-store-service-connector, \
            commons/utils/tickle-harvester-service-connector, \
            gatekeeper, \
            job-processor2, \
            dlq-errorhandler, \
            sink/dummy, \
            sink/marcconv, \
            sink/dmat, \
            sink/worldcat, \
            sink/diff, \
            sink/holdings-items, \
            sink/vip, \
            sink/ims, \
            sink/tickle-repo, \
            sink/openupdate, \
            sink/batch-exchange, \
            sink/periodic-jobs, \
            sink/dpf \
        "
    }
    triggers {
        upstream(upstreamProjects: "Docker-payara5-bump-trigger",
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
                    test -d '/home/isworker/.dataio_repo/dk/dbc' && rm -r '/home/isworker/.dataio_repo/dk/dbc'
                """
                checkout scm
            }
        }
        stage("build") {
            steps {
                sh """
                mvn -B -T 6 install
                mvn -B -T 6 pmd:pmd
                echo Build CLI for \$BRANCH_NAME \$BUILD_NUMBER
                ./cli/build_docker_image.sh
            """
                script {
                    junit testResults: '**/target/*-reports/*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    publishIssues issues:[java], unstableTotalAll:1

                    def pmd = scanForIssues tool: [$class: 'Pmd']
                    publishIssues issues:[pmd], unstableTotalAll:1

                    def spotbugs = scanForIssues tool: [$class: 'SpotBugs']
                    publishIssues issues:[spotbugs], unstableTotalAll:1

                    archiveArtifacts artifacts: "docker-images.log,cli/acceptance-test/target/dataio-cli-acctest.jar,gatekeeper/target/dataio-gatekeeper*.jar,cli/dataio-cli",
                            fingerprint: true
                }
            }
        }
        stage("docker push") {
            when {
                branch "master"
            }
            steps {
                sh """
                cat docker-images.log | parallel -j 3 docker push {}:master-${env.BUILD_NUMBER}
                docker tag docker-metascrum.artifacts.dbccloud.dk/gatekeeper-staging:devel docker-metascrum.artifacts.dbccloud.dk/gatekeeper-staging:DIT-${env.BUILD_NUMBER}
                docker tag docker-metascrum.artifacts.dbccloud.dk/gatekeeper-jmx-exporter:devel docker-metascrum.artifacts.dbccloud.dk/gatekeeper-jmx-exporter:DIT-${env.BUILD_NUMBER}
                docker push docker-metascrum.artifacts.dbccloud.dk/gatekeeper-staging:DIT-${env.BUILD_NUMBER}
                docker push docker-metascrum.artifacts.dbccloud.dk/gatekeeper-jmx-exporter:DIT-${env.BUILD_NUMBER}
            """
                script {
                    stash includes: "docker-images.log", name: docker_images_log_stash_tag
                    archiveArtifacts "docker-images.log"
                }
            }
        }
        stage("deploy to mavenrepo.dbc.dk") {
            when {
                branch "master"
            }
            steps {
                sh """
                mvn deploy -B -Dmaven.test.skip=true -Pdocker-push -am -pl "${DEPLOY_ARTIFACTS}"
            """
            }
        }
        stage("promote to DIT") {
            when {
                branch "master"
            }
            steps {
                dir("docker") {
                    unstash docker_images_log_stash_tag
                    sh """
                    cat docker-images.log | parallel -j 3 docker tag {}:master-${env.BUILD_NUMBER} {}:DIT-${env.BUILD_NUMBER}
                    cat docker-images.log | parallel -j 3 docker push {}:DIT-${env.BUILD_NUMBER}
                """
                }
            }
        }
        stage("Clean up docker images") {
            when {
                branch "master"
            }
            steps {
                sh """
                ./docker/remove-images docker-metascrum.artifacts.dbccloud.dk/dbc-payara-*
            """
            }
        }
        stage("bump docker tags in dataio-secrets") {
            agent {
                docker {
                    label workerNode
                    image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                    set-new-version services ${env.GITLAB_PRIVATE_TOKEN} metascrum/dataio-secrets DIT-${env.BUILD_NUMBER} -b staging
                """
                }
            }
        }
        stage("bump docker tags in dit-gitops-secrets") {
            agent {
                docker {
                    label workerNode
                    image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                    set-new-version services/dataio-project ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets DIT-${env.BUILD_NUMBER} -b master
                """
                }
            }
        }
        stage("deploy this branch to staging?") {
            when {
                allOf {
                    not {
                        branch "master"
                    }
                    not {
                        branch "PR-*"
                    }
                }

            }
            steps {
                script {
                    def deployBranchToStaging = input(message: 'Vil du deploye dette byg til staging DataIO?', ok: 'Yes',
                            parameters: [booleanParam(defaultValue: true,
                                    description: 'Dette byg bliver deployet til staging', name: 'Jep')])
                }
                sh """
            mvn deploy -B -Dmaven.test.skip=true -Pdocker-push -Dtag="${env.BRANCH_NAME}-${env.BUILD_NUMBER}" -am -pl "${DEPLOY_ARTIFACTS}"
            cat docker-images.log | parallel -j 3  docker push {}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}
        """
            }
        }
        stage("bump docker tags in dataio-secrets for non-master branches") {
            agent {
                docker {
                    label workerNode
                    image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                allOf {
                    not {
                        branch "master"
                    }
                    not {
                        branch "PR-*"
                    }
                }

            }
            steps {
                script {
                    sh """
                    set-new-version services ${env.GITLAB_PRIVATE_TOKEN} metascrum/dataio-secrets ${env.BRANCH_NAME}-${env.BUILD_NUMBER} -b staging
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
