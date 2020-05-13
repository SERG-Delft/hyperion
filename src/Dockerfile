# Example gradle builder
# Note: project root should be used as build context
FROM gradle:6.3-jdk14 as gradle

# Use build layer as a partial cache
FROM gradle as cache
RUN mkdir -p /home/gradle/cache
ENV GRADLE_USER_HOME /home/gradle/cache

# Future work: create seperate glob script to get all .gradle files
COPY build.gradle.kts settings.gradle.kts /home/gradle/project/
COPY aggregator/build.gradle.kts /home/gradle/project/aggregator/
COPY renamer/build.gradle.kts /home/gradle/project/renamer/
COPY pluginmanager/build.gradle.kts /home/gradle/project/pluginmanager/
COPY datasource/plugins/elasticsearch/build.gradle.kts /home/gradle/project/datasource/plugins/elasticsearch/

WORKDIR /home/gradle/project
RUN gradle -i build

# Build project artifacts
FROM gradle
COPY --from=cache /home/gradle/cache /home/gradle/.gradle
COPY --chown=gradle:gradle . .
RUN gradle -i --no-daemon :aggregator:shadowJar :datasource:plugins:elasticsearch:shadowJar \
    :extractor:shadowJar :pluginmanager:shadowJar :renamer:shadowJar :sampleplugin:shadowJar