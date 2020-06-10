# Example gradle builder
# Note: project root should be used as build context
FROM gradle:6.3-jdk14 as gradle

FROM gradle
RUN mkdir -p /home/gradle/cache
ENV GRADLE_USER_HOME /home/gradle/cache
WORKDIR /home/gradle/project/
CMD gradle -i --no-daemon :pluginmanager:shadowJar :pipeline:plugins:stresser:shadowJar :pipeline:plugins:rate:shadowJar
