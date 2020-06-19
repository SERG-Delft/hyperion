# first build the jar
# assumes build context is the root of this project
FROM gradle:jdk14 as builder
COPY . .
RUN gradle pipeline:plugins:pathextractor:shadowJar

# run the built jar
FROM openjdk:14 AS runner
COPY --from=builder /home/gradle/pipeline/plugins/pathextractor/build/pathextractor-all.jar .
CMD touch "/root/config.yml"
CMD java -jar pathextractor-all.jar "/root/config.yml"
