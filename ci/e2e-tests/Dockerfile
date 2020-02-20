# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

# This stage depends on a JDK image and uses Maven Wrapper to pull in dependencies and build the project
# from source.
FROM openjdk:8-jdk-slim AS build

ENV MAVEN_OPTS "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
ARG APP_NAME
ARG APP_VERSION

WORKDIR /build

# Before bringing in project sources, resolve maven dependencies.
# This allows Docker to reuse the cached image with all dependencies resolved.
ADD target/pom.xml ./
ADD target/.mvn ./.mvn
ADD target/mvnw ./
RUN ./mvnw -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
RUN ./mvnw -B versions:set -DnewVersion=${APP_VERSION}

# Now add sources, which will bust the cache.
ADD target/src ./src
RUN ./mvnw -B -o package -DskipTests=true

##
## Stage 2: Package the runnable test image
##

# This stage uses a JRE rather than JDK because it doesn't have to compile any Java sources.
# It is assembling the runnable test container and packaging all its Maven dependencies
FROM selenium/standalone-chrome:3.141.59-xenon

ENV MAVEN_OPTS "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"

COPY --from=0 --chown=seluser:seluser /build/target/e2e-tests.jar /e2e-tests.jar
COPY --chown=seluser:seluser target/e2e-exec-pom.xml /pom.xml
COPY --chown=seluser:seluser target/.mvn /.mvn
COPY --chown=seluser:seluser target/mvnw /

USER root
RUN mkdir /target \
	&& chown seluser:seluser /target
USER seluser:seluser

# Tests use E2E_SCREENSHOT_DIR at runtime to decide where to save screenshot files
# We are using the Maven output directory because it's the only place where seluser can create files
ENV E2E_SCREENSHOT_DIR /target

RUN ./mvnw -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

ENTRYPOINT ["./mvnw", "-B", "-o", "test"]
