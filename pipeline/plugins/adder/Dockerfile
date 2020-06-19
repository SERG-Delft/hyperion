# first build the jar
# assumes build context is the root of this project
FROM gradle:jdk14 as builder
COPY . .
RUN gradle pipeline:plugins:adder:shadowJar

# run the built jar
FROM openjdk:14 AS runner
COPY --from=builder /home/gradle/pipeline/plugins/adder/build/adder-all.jar .
CMD touch "/root/config.yml"
CMD java -jar adder-all.jar "/root/config.yml"
