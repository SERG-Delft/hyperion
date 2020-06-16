FROM openjdk:14
COPY build/stresser-all.jar .
CMD touch "/root/config.yml"
CMD java -jar stresser-all.jar "/root/config.yml"
