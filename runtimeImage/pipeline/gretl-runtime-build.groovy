/**
 * Builds and tests the GRETL jar and starts an OpenShift Docker build.
 * The Docker image will be pushed to the repository with the build number as tag.
 * Needed parameter:
 * - repository: repository incl. user / organisation to push the Docker image to (text parameter)
 * - openShiftCluster: OpenShift Cluster to be used. (text parameter)
 * - openShiftProject: OpenShift project to be used (text parameter)
 * - ocToolName: Jenkins custom tool name of oc client (text parameter)
 * - openShiftDeployTokenName: amount of tags to keep (text parameter)
 */

pipeline {
    agent any
    tools {
        'jdk' "OpenJDK 1.8"
    }
    stages {
        stage('check parameter') {
            steps {
                script {
                    check.mandatoryParameter('repository')
                    check.mandatoryParameter('openShiftCluster')
                    check.mandatoryParameter('openShiftProject')
                    check.mandatoryParameter('ocToolName')
                    check.mandatoryParameter('openShiftDeployTokenName')
                }
            }
        }
        stage('jar build') {
            steps {
                // output versions
                sh './gradlew -version'

                // build and test jar
                sh './gradlew clean build --refresh-dependencies'

                // run db tests
                script {
                    // find database pod with name: postgresql
                    def podSelector = openshift.selector('pods', [name: 'postgres-gis'])
                    def dbPod = podSelector.name()
                    String shortName = dbPod.indexOf("/") > 0 ? dbPod.substring(dbPod.indexOf("/") + 1) : dbPod;
                    println "db pod: " + shortName

                    // prepare database
                    openshift.raw("cp", "runtimeImage/pipeline/scripts/reset-test-db.sh", "${shortName}:/tmp/")
                    echo openshift.exec(shortName, 'bash', '/tmp/reset-test-db.sh').out

                    openshift.raw("cp", "runtimeImage/pipeline/scripts/init-test-db.sh", "${shortName}:/tmp/")
                    echo openshift.exec(shortName, 'bash', '/tmp/init-test-db.sh').out

                    parallel(
                        port_forward: {
                            int buildTime = 1
                            if (params.buildTime != null) {
                                buildTime = params.buildTime as int
                            }
                            try {
                                timeout(buildTime) {
                                    echo "forward database port"
                                    openshift.raw('port-forward', shortName, '5432:5432')
                                }
                            } catch (err) {
                            } // catch timeout
                            println "port forward done (time: ${buildTime})"
                        },
                        test: {
                            echo "run db tests ..."
                            dir('build-tmp') {
                                sh './gradlew -Ddburl=jdbc:postgresql:gretl -Ddbusr=postgres -Ddbpwd=password build dbTest'
                            }
                        }
                    )
                }
            }
            post {
                always {
                    junit 'gretl/build/test-results/**/*.xml'  // Requires JUnit plugin
                }
                success {
                    archiveArtifacts 'gretl/build/libs/*.?ar'
                }
            }
        }
        stage('prepare') {
            steps {
                sh 'rm -rf build-tmp'
                sh 'mkdir build-tmp'

                sh 'cp -R gretl/build/libs/* build-tmp'
                sh 'cp -R runtimeImage/gretl/* build-tmp'
                sh 'cp -R dependencies.gradle build-tmp'

                dir('build-tmp') {
                    sh "echo 'jenkins build' > build.info"
                    sh "echo date: `date '+%Y-%m-%d %H:%M:%S'` >> build.info"
                    sh "echo BUILD_NUMBER: ${BUILD_NUMBER} >> build.info"
                    sh "echo BUILD_TAG: ${BUILD_TAG} >> build.info"
                }

                sh 'ls -la build-tmp'
            }
        }
        stage('OpenShift build') {
            steps {
                script{
                    timeout(20) {
                        def ocDir = tool params.ocToolName
                        withEnv(["PATH+OC=${ocDir}"]) {
                            sh "oc version"
                            openshift.withCluster(params.openShiftCluster, params.openShiftDeployTokenName) {
                                openshift.withProject(params.openShiftProject) {
                                    echo "Running in project: ${openshift.project()}"

                                    // update docker image tag with build number
                                    def imageRef = "docker.io/${params.repository}:${BUILD_NUMBER}"
                                    def bc = openshift.selector('bc/gretl').object()
                                    bc.spec.output.to['name'] = imageRef
                                    openshift.apply(bc)


                                    def builds = openshift.startBuild("gretl","--from-dir=./build-tmp/")
                                    builds.untilEach(1) {
                                        echo "Created builds so far: ${it.names()}"

                                        return it.object().status.phase == "Complete" || it.object().status.phase == "Cancelled" || it.object().status.phase == "Failed"
                                    }

                                    echo "created and pushed image: ${imageRef}"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
