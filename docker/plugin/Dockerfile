FROM ubuntu
RUN apt-get update && apt-get install -y default-jdk
COPY plugin-all.jar .
CMD java -jar plugin-all.jar
