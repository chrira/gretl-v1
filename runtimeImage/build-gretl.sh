#!/bin/bash

echo "======================================================================="
echo "Build Docker container for GRETL runtime"
echo "Uses the following container definition:"
echo "https://github.com/openshift/jenkins/blob/master/slave-maven/Dockerfile"
echo "======================================================================="



../gradlew stageJars #copy all jar dependencies to __jars4image
cp ../gretl/build/libs/gretl-1.0.4-SNAPSHOT.jar gretl/__jars4image
cp ../gretl/lib/ojdbc7-*.jar gretl/__jars4image

# Set the image labels to the given shell params or sensitive defaults

githash=$1
if [ "x$githash" = "x" ]; then
    githash='localbuild'
fi

buildident=$2
if [ "x$buildident" = "x" ]; then
    buildident='localbuild'
fi

build_timestamp=$(date '+%Y-%m-%d_%H:%M:%S')

docker build \
    --no-cache --force-rm -t sogis/gretl-runtime:$buildident \
    --label gretl.created=$build_timestamp --label gretl.git_commit=$githash --label gretl.travis_build=$buildident \
    -f gretl/Dockerfile gretl

rm gretl/__jars4image/*

# look into the container:
# docker run -it --entrypoint=/bin/sh sogis/gretl-runtime:$buildident

