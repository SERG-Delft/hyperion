# first build the jar
# assumes build context is the root of this project
FROM gradle:jdk14 as builder
COPY . .
RUN gradle datasource:plugins:elasticsearch:shadowJar

# run the built jar
FROM openjdk:14 AS runner
COPY --from=builder /home/gradle/datasource/plugins/elasticsearch/build/elasticsearch-all.jar .
CMD touch "/root/config.yml"
CMD java -jar elasticsearch-all.jar run "/root/config.yml"