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
    agent {label workerNode}
    tools {
        // refers to the name set in manage jenkins -> global tool configuration
        maven "Maven 3"
    }
    environment {
        MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true"
        ARTIFACTORY_LOGIN = credentials("artifactory_login")
        SONARQUBE_HOST = "http://sonarqube.mcp1.dbc.dk"
        SONARQUBE_TOKEN = credentials("dataio-sonarqube")
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
        stage("update submodules") {
            steps {
                sh "git submodule update --init"
            }
        }
        stage("start server") {
            steps {
                sh "./handle_server_docker start"
                stash includes: "handle_server_docker,it-docker-container-ids", name: docker_containers_stash_tag
            }
        }
        stage("build") {
            steps {
                sh """
                    mvn -B clean
                    mvn -B -P !integration-test -T 6 install
                    mvn -B -P !integration-test -T 6 pmd:pmd
                    mvn -B javadoc:aggregate -pl !commons/query-language
                    mvn -B -f integration-test/pom.xml verify
                """
                junit "**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml"
                archiveArtifacts artifacts: "cli/acceptance-test/target/dataio-cli-acctest.jar,gatekeeper/target/dataio-gatekeeper*.jar,cli/dataio-cli",
                    fingerprint: true
            }
        }
        stage("sonarqube") {
            when {
                branch "master"
            }
            steps {
                script {
                    try {
                        sh """
                            mvn sonar:sonar \
                            -Dsonar.host.url=$SONARQUBE_HOST \
                            -Dsonar.login=$SONARQUBE_TOKEN
                            -Dsonar.scm.provider=git
                        """
                    } catch(e) {
                        printf "sonarqube connection failed: %s", e.toString()
                    }
                }
            }
        }
        stage("docker pull") {
            steps {
                sh """
                    docker pull docker-i.dbc.dk/dbc-payara:latest
                    docker pull docker-io.dbc.dk/dbc-payara-logback:latest
                """
            }
        }
        stage("docker build") {
            environment {
                PUSH = "dontpush"
            }
            steps {
                sh """
                    rm -f docker-images.log
                """
                dir("docker") {
                    script {
                        if(env.BRANCH_NAME == "master") PUSH = "--push"
                    }
                    sh """
                        ./build-all-images $PUSH
                        ./remove-images docker-io.dbc.dk/dbc-payara-*
                    """
                }
                script {
                    if(env.BRANCH_NAME == "master") {
                        stash includes: "docker-images.log", name: docker_images_log_stash_tag
                        archiveArtifacts "docker-images.log"
                    }
                }
                // Clashes with iScrum containers
                // ./docker/remove-dangling-images
            }
        }
        stage("warnings") {
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
            steps {
                step([$class: 'hudson.plugins.pmd.PmdPublisher',
                    pattern: '**/target/pmd.xml',
                    unstableTotalAll: "0",
                    failedTotalAll: "0"])
            }
        }
        stage("promote to DIT") {
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
        stage("deploy staging") {
            when {
                branch "master"
            }
            steps {
                build job: "dataio/dataio-deploy-staging/master", wait: false, parameters: [
                    string(name: "dockerTag", value: "DIT-${env.BUILD_NUMBER}")
                ]
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
            node(workerNode) {
                unstash docker_containers_stash_tag
                sh "./handle_server_docker stop"
            }
        }
    }
}
