#!groovy

def docker_containers_stash_tag = "docker_container_ids"

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
    agent { label "itwn-002" }
    tools {
        // refers to the name set in manage jenkins -> global tool configuration
        maven "Maven 3"
    }
    environment {
        MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true"
    }
    triggers {
        pollSCM("H/3 * * * *")
    }
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: "",
            artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
    }
    stages {
        stage("start server") {
            steps {
                sh "./handle_server_docker start"
                stash includes: "it-docker-container-ids", name: docker_containers_stash_tag
            }
        }
        stage("build") {
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
            steps {
                sh """
                    docker pull docker-i.dbc.dk/dbc-payara:latest
                    docker pull docker-io.dbc.dk/dbc-payara-logback:latest
                """
            }
        }
        stage("docker build") {
            steps {
                sh """
                    rm -f docker-images.log
                    ./docker/build-all-images
                    ./docker/remove-images docker-io.dbc.dk/dbc-payara-*
                """
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
