FROM openjdk:14
COPY build/extractor-all.jar .
CMD touch "/root/config.yml"
CMD java -jar extractor-all.jar "/root/config.yml"