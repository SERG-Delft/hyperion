# Example gradle builder
# Note: project root should be used as build context
FROM gradle:6.3-jdk14 as gradle

# Use build layer as a partial cache
#FROM gradle as cache
#RUN mkdir -p /home/gradle/cache
#ENV GRADLE_USER_HOME /home/gradle/cache

## Future work: create seperate glob script to get all .gradle files
#COPY build.gradle.kts settings.gradle.kts /home/gradle/build_files/
#COPY aggregator/build.gradle.kts /home/gradle/build_files/aggregator/
#COPY renamer/build.gradle.kts /home/gradle/build_files/renamer/
#COPY pluginmanager/build.gradle.kts /home/gradle/build_files/pluginmanager/
#COPY datasource/plugins/elasticsearch/build.gradle.kts /home/gradle/build_files/datasource/plugins/elasticsearch/

#WORKDIR /home/gradle/build_files
#RUN gradle -i build

# Build project artifacts
#FROM gradle
#COPY --from=cache /home/gradle/cache /home/gradle/.gradle
#RUN gradle -i --no-daemon :aggregator:shadowJar :datasource:plugins:elasticsearch:shadowJar \
#    :extractor:shadowJar :pluginmanager:shadowJar :renamer:shadowJar :sampleplugin:shadowJar

FROM gradle
RUN mkdir -p /home/gradle/cache
ENV GRADLE_USER_HOME /home/gradle/cache
WORKDIR /home/gradle/project/
CMD gradle -i --no-daemon :aggregator:shadowJar :datasource:plugins:elasticsearch:shadowJar \
    :pluginmanager:shadowJar :pipeline:plugins:renamer:shadowJar :pipeline:plugins:adder:shadowJar \
    :pipeline:plugins:pathextractor:shadowJar
