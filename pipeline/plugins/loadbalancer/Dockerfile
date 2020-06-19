# first build the jar
# assumes build context is the root of this project
FROM gradle:jdk14 as builder
COPY . .
RUN gradle pipeline:plugins:loadbalancer:shadowJar

# run the built jar
FROM openjdk:14 AS runner
COPY --from=builder /home/gradle/pipeline/plugins/loadbalancer/build/loadbalancer-all.jar .
CMD touch "/root/config.yml"
CMD java -jar loadbalancer-all.jar "/root/config.yml"
