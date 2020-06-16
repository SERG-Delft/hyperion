FROM openjdk:14
COPY build/loadbalancer-all.jar .
CMD touch "/root/config.yml"
CMD java -jar loadbalancer-all.jar "/root/config.yml"
