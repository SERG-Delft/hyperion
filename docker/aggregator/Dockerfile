FROM ubuntu
RUN apt-get update && apt-get install -y default-jdk
COPY aggregator-all.jar .
CMD java -jar aggregator-all.jar
