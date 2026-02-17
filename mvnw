#!/bin/sh
# Maven Wrapper script
# Auto-downloads Maven if not present

set -e

# Determine the CUAS-Eval project dir
WDIR="$(cd "$(dirname "$0")" && pwd)"
MAVEN_PROJECTBASEDIR="${MAVEN_PROJECTBASEDIR:-$WDIR}"

MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

# Read distribution URL
if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    distributionUrl=$(grep 'distributionUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
fi

MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-3.9.6"

if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven..."
    mkdir -p "$MAVEN_HOME"
    tmpZip="$MAVEN_HOME/maven.zip"
    if command -v curl > /dev/null; then
        curl -fsSL -o "$tmpZip" "$distributionUrl"
    elif command -v wget > /dev/null; then
        wget -q -O "$tmpZip" "$distributionUrl"
    fi
    unzip -q "$tmpZip" -d "$MAVEN_HOME"
    rm -f "$tmpZip"
fi

MVN_CMD="$(find "$MAVEN_HOME" -name 'mvn' -path '*/bin/mvn' | head -1)"
exec "$MVN_CMD" "$@"
