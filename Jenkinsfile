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
            steps {
                sh "./handle_server_docker start"
                stash includes: "handle_server_docker,it-docker-container-ids", name: docker_containers_stash_tag
            }
        }
        stage("build") {
            steps {
                sh """
                    rm -f docker-images.log
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
                    ./docker/remove-images docker-io.dbc.dk/dbc-payara-*
                """
                script {
                    stash includes: "docker-images.log", name: docker_images_log_stash_tag
                    archiveArtifacts "docker-images.log"
                }
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
                    sh "cat docker-images.log | parallel -j 3 ./remote-tag.py --username ${ARTIFACTORY_LOGIN_USR} --password ${ARTIFACTORY_LOGIN_PSW} {} master-${env.BUILD_NUMBER} DIT-${env.BUILD_NUMBER}"
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
            node(workerNode) {
                unstash docker_containers_stash_tag
                sh "./handle_server_docker stop"
            }
        }
    }
}
