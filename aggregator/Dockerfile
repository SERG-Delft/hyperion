# first build the jar
# assumes build context is the root of this project
FROM gradle:jdk14 as builder
COPY . .
RUN gradle aggregator:shadowJar

# run the built jar
FROM openjdk:14 AS runner
COPY --from=builder /home/gradle/aggregator/build/aggregator-all.jar .
CMD touch "/root/config.yml"
CMD java -jar aggregator-all.jar "/root/config.yml"