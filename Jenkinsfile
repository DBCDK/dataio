#!groovy

def docker_images_log_stash_tag = "docker_images_log"
def workerNode = "devel10"

pipeline {
    agent {label workerNode}
    tools {
		jdk 'jdk11'
		maven 'Maven 3'
    }
    environment {
        MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true"
        ARTIFACTORY_LOGIN = credentials("artifactory_login")
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
    }
    triggers {
        upstream(upstreamProjects: "Docker-payara5-bump-trigger",
			threshold: hudson.model.Result.SUCCESS)
    }
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: "",
            artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
        timeout(time: 2, unit: "DAYS")
    }
    stages {
        stage("build") {
            steps {
                sh """
                    rm -f docker-images.log
                    mvn -B clean
                    mvn -B -T 6 install
                    mvn -B -P !integration-test -T 6 pmd:pmd
                    mvn -B javadoc:aggregate -pl !commons/query-language
                    ./cli/build_docker_image.sh
                """
                junit "**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml"
                archiveArtifacts artifacts: "docker-images.log,cli/acceptance-test/target/dataio-cli-acctest.jar,gatekeeper/target/dataio-gatekeeper*.jar,cli/dataio-cli",
                    fingerprint: true
            }
        }
        stage("warnings") {
            steps {
                warnings consoleParsers: [
                    [parserName: "Java Compiler (javac)"]
                ],
                unstableTotalAll: "0",
                failedTotalAll: "0"
            }
        }
        stage("PMD") {
            steps {
                step([$class: 'hudson.plugins.pmd.PmdPublisher',
                    pattern: '**/target/pmd.xml',
                    unstableTotalAll: "0",
                    failedTotalAll: "0"])
            }
        }
        stage("docker push") {
            when {
                branch "master"
            }
            steps {
                sh """
                    cat docker-images.log | parallel -j 3 docker push {}:master-${env.BUILD_NUMBER}
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
                    mvn deploy -Dmaven.test.skip=true -am -pl commons/utils/flow-store-service-connector -pl commons/utils/tickle-harvester-service-connector
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
    }
}
