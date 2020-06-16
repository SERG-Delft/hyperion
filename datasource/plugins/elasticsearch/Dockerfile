FROM openjdk:14
COPY build/elasticsearch-all.jar .
CMD touch "/root/config.yml"
CMD java -jar elasticsearch-all.jar run "/root/config.yml"