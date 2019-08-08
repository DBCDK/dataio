#!groovy

def docker_images_log_stash_tag = "docker_images_log"
def workerNode = "itwn-002"

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
                    mvn -B -P !integration-test -T 6 install
                    mvn -B -P !integration-test -T 6 pmd:pmd
                    mvn -B javadoc:aggregate -pl !commons/query-language
                    mvn -B -f integration-test/pom.xml verify
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
                    sh "cat docker-images.log | parallel -j 3 ./remote-tag.py --username ${ARTIFACTORY_LOGIN_USR} --password ${ARTIFACTORY_LOGIN_PSW} {} master-${env.BUILD_NUMBER} DIT-${env.BUILD_NUMBER}"
                }
            }
        }
    }
}
