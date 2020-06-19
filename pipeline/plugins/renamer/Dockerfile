# first build the jar
# assumes build context is the root of this project
FROM gradle:jdk14 as builder
COPY . .
RUN gradle pipeline:plugins:renamer:shadowJar

# run the built jar
FROM openjdk:14 AS runner
COPY --from=builder /home/gradle/pipeline/plugins/renamer/build/renamer-all.jar .
CMD touch "/root/config.yml"
CMD java -jar renamer-all.jar "/root/config.yml"
