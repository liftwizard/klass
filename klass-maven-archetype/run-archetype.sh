#!/usr/bin/env bash

set -euxo pipefail

mvn archetype:generate \
  -DarchetypeCatalog=local \
  -DarchetypeGroupId=cool.klass \
  -DarchetypeArtifactId=klass-maven-archetype \
  -DinteractiveMode=false \
  -DgroupId=cool.klass.xample.coverage \
  -DartifactId=coverage-example \
  -Dversion=0.1.0-SNAPSHOT \
  -Dpackage=cool.klass.xample.coverage \
  -Dname=CoverageExample

cd coverage-example

git init
git add -A
git commit -m "Initial import."

mvn wrapper:wrapper -Dmaven=4.0.0-alpha-7

./mvnw clean verify -Dmaven.build.cache.skipCache=true --activate-profiles dev,rerecord
