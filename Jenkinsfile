#!groovy

def docker_containers_stash_tag = "docker_container_ids"
def docker_images_log_stash_tag = "docker_images_log"
def workerNode = "itwn-002"

void notifyOfBuildStatus(final String buildStatus) {
    final String subject = "${buildStatus}: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    final String details = """<p> Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""
    emailext(
        subject: "$subject",
        body: "$details", attachLog: true, compressLog: false,
        mimeType: "text/html",
        recipientProviders: [[$class: "CulpritsRecipientProvider"]]
    )
}

pipeline {
    agent none
    tools {
        // refers to the name set in manage jenkins -> global tool configuration
        maven "Maven 3"
    }
    environment {
        MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true"
        ARTIFACTORY_LOGIN = credentials("artifactory_login")
    }
    triggers {
        pollSCM("H/3 * * * *")
    }
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: "",
            artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
        timeout(time: 2, unit: "DAYS")
    }
    stages {
        stage("start server") {
            agent {label workerNode}
            steps {
                sh "./handle_server_docker start"
                stash includes: "it-docker-container-ids", name: docker_containers_stash_tag
            }
        }
        stage("build") {
            agent {label workerNode}
            steps {
                sh """
                    mvn -B clean
                    mvn -B -P !integration-test -T 6 install
                    mvn -B -P !integration-test -T 6 pmd:pmd
                    mvn -B javadoc:aggregate
                    mvn -B -f integration-test/pom.xml verify
                """
                junit "**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml"
            }
        }
        stage("docker pull") {
            agent {label workerNode}
            steps {
                sh """
                    docker pull docker-i.dbc.dk/dbc-payara:latest
                    docker pull docker-io.dbc.dk/dbc-payara-logback:latest
                """
            }
        }
        stage("docker build") {
            agent {label workerNode}
            environment {
                PUSH = "dontpush"
            }
            steps {
                dir("docker") {
                    script {
                        if(env.BRANCH_NAME == "master") PUSH = "--push"
                    }
                    sh """
                        rm -f docker-images.log
                        ./build-all-images $PUSH
                        ./remove-images docker-io.dbc.dk/dbc-payara-*
                    """
                }
                script {
                    if(env.BRANCH_NAME == "master") {
                        stash includes: "docker-images.log", name: docker_images_log_stash_tag
                    }
                }
                // Clashes with iScrum containers
                // ./docker/remove-dangling-images
            }
        }
        stage("warnings") {
            agent {label workerNode}
            steps {
                warnings consoleParsers: [
                    [parserName: "Java Compiler (javac)"],
                    [parserName: "JavaDoc Tool"]
                ],
                unstableTotalAll: "0",
                failedTotalAll: "0"
            }
        }
        stage("PMD") {
            agent {label workerNode}
            steps {
                step([$class: 'hudson.plugins.pmd.PmdPublisher',
                    pattern: '**/target/pmd.xml',
                    unstableTotalAll: "0",
                    failedTotalAll: "0"])
            }
        }
        stage("ask for promotion to DIT") {
            when {
                branch "master"
            }
            steps {
                milestone label: "askForDITPromotion", ordinal: 1
                input message: "tag with DIT-${env.BUILD_NUMBER}?"
                milestone label: "askedForDITPromotion", ordinal: 2
            }
        }
        stage("promote to DIT") {
            agent {label workerNode}
            when {
                branch "master"
            }
            steps {
                dir("docker") {
                    unstash docker_images_log_stash_tag
                    sh "echo \"docker-io.dbc.dk/dataio-cli\" >> docker-images.log"
                    sh "cat docker-images.log | parallel -j 3 ./remote-tag.py --username ${ARTIFACTORY_LOGIN_USR} --password ${ARTIFACTORY_LOGIN_PSW} {} latest DIT-${env.BUILD_NUMBER}"
                }
            }
        }
    }
    post {
        unstable {
            notifyOfBuildStatus("build became unstable")
        }
        failure {
            notifyOfBuildStatus("build failed")
        }
        always {
            unstash docker_containers_stash_tag
            sh "./handle_server_docker stop"
        }
    }
}
