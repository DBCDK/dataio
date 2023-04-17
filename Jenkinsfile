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
        MAVEN_OPTS="-Dmaven.repo.local=.repo -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
        ARTIFACTORY_LOGIN = credentials("artifactory_login")
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
        BUILD_NUMBER="${env.BUILD_NUMBER}"
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
        disableConcurrentBuilds()
    }
    stages {
        stage('clean and checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }
        stage("build") {
            steps {
                sh """
                    mvn -B -T 6 install
                    mvn -B -P !integration-test -T 6 pmd:pmd
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
                    mvn deploy -B -Dmaven.test.skip=true -Pdocker-push -am -pl "commons/utils/flow-store-service-connector, commons/utils/tickle-harvester-service-connector, gatekeeper, job-processor2, sink/dummy"
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
                        set-new-version services ${env.GITLAB_PRIVATE_TOKEN} metascrum/dataio-secrets DIT-${env.BUILD_NUMBER} -b staging
                    """
                }
            }
        }
        stage("bump docker tags in dit-gitops-secrets") {
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
                        set-new-version services/dataio-project ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets DIT-${env.BUILD_NUMBER} -b master
                    """
                }
            }
        }
        stage("clean up successful build") {
            steps {
                cleanWs()
            }
        }
    }
}
