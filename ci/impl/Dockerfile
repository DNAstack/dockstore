# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

# This stage depends on a JDK image and uses Maven Wrapper to pull in dependencies and build the project
# from source.
FROM openjdk:11-jdk-slim AS build
ARG APP_NAME
ARG APP_VERSION

WORKDIR /build

# Before bringing in project sources, resolve maven dependencies.
# This allows Docker to reuse the cached image with all dependencies resolved.
ADD target/pom.xml ./
ADD target/.mvn ./.mvn
ADD target/mvnw ./
ADD target/checkstyle.xml ./
ADD target/checkstyle-suppressions.xml ./
# Allows us to pass settings.xml configured on local machine or CI server to access private Nexus repo
ADD target/.m2 /root/.m2
ADD target/dockstore-webservice/pom.xml ./dockstore-webservice/
ADD target/dockstore-language-plugin-parent/pom.xml ./dockstore-language-plugin-parent/
ADD target/dockstore-common/pom.xml ./dockstore-common/
ADD target/swagger-java-client/pom.xml ./swagger-java-client/
ADD target/openapi-java-client/pom.xml ./openapi-java-client/
ADD target/swagger-java-quay-client/pom.xml ./swagger-java-quay-client/
ADD target/swagger-java-quay-client/discovery.formatted.json ./swagger-java-quay-client/
ADD target/swagger-java-bitbucket-client/pom.xml ./swagger-java-bitbucket-client/
ADD target/swagger-java-bitbucket-client/swagger.yaml ./swagger-java-bitbucket-client/


ADD target/swagger-java-sam-client/pom.xml ./swagger-java-sam-client/
ADD target/swagger-java-discourse-client/pom.xml ./swagger-java-discourse-client/
ADD target/swagger-java-zenodo-client/pom.xml ./swagger-java-zenodo-client/

# Resolves all maven dependencies
#RUN ./mvnw -B -Presolve-dependencies initialize
RUN ls -alR
#RUN ./mvnw dependency:resolve
RUN ./mvnw -Presolve-dependencies initialize
RUN ./mvnw -B versions:set -DnewVersion=${APP_VERSION}
#RUN cd dockstore-webservice && ../mvnw -Presolve-dependencies initialize && cd ..

# Now add sources, which will bust the cache.
ADD target/dockstore-webservice/src ./dockstore-webservice/src
ADD target/dockstore-common/src ./dockstore-common/src
ADD target/swagger-java-client/src ./swagger-java-client/src
ADD target/swagger-java-discourse-client/src ./swagger-java-discourse-client/src
ADD target/openapi-java-client/src ./openapi-java-client/src
ADD target/swagger-java-zenodo-client/src ./swagger-java-zenodo-client/src
ADD target/dockstore-language-plugin-parent/src ./dockstore-language-plugin-parent/src
# If this step fails with an error like
#     Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:2.22.1:test
#     (default-test) on project xxx: Unable to generate classpath:
#     org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException: Missing:
# then update the version number of surefire-junit4 in pom.xml under the
# dynamicDependencies section of go-offline-maven-plugin configuration.
#RUN cd dockstore-webservice && ./mvnw -B -o package && cd ..
RUN ./mvnw -B -o package -DskipTests

##
## Stage 2: Package the runnable image
##

# This stage uses a JRE rather than JDK because it doesn't have to build anything; it just runs the fat jar
# produced by the previous stage.
FROM openjdk:11-slim
ARG APP_NAME
ARG APP_VERSION

COPY --from=0 /build/dockstore-webservice/target/dockstore-webservice-${APP_VERSION}.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "server", "dockstore-webservice/dockstore.yml"]
