FROM maven:3.6.2-jdk-11 AS maven

# Preinitialize to take better advantage of docker layer caching, and avoid redownloading dependencies
# when the pom has not changed.
ADD dockstore-webservice/pom.xml /dockstore-webservice/pom.xml
RUN cd dockstore-webservice && mvn -B -Presolve-dependencies initialize

COPY . /

#RUN cd dockstore-webservice && mvn clean install -DskipTests && cd ..
RUN cd dockstore-webservice && mvn -B -o package && cd ..
FROM ubuntu:18.04

# Update the APT cache
# prepare for Java download
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
    software-properties-common \
    telnet \
    vim \
    wget \
    locales \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# install java
RUN add-apt-repository ppa:openjdk-r/ppa
RUN apt-get update \
    && apt-get install openjdk-11-jdk=11.0.5+10-0ubuntu1.1~18.04 -y --no-install-recommends \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN locale-gen en_US.UTF-8
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

COPY --from=maven /dockstore-webservice/target/d*SNAPSHOT.jar /home

# install dockerize
ENV DOCKERIZE_VERSION v0.2.0

RUN curl -L https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    | tar -C /usr/local/bin -xzv

RUN mkdir /dockstore_logs && chmod a+rx /dockstore_logs

# Waiting for postgres service
CMD ["dockerize", "-wait", "tcp://postgres:5432", "-timeout", "60s", "/home/init_webservice.sh"]

