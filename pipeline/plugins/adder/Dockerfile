FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/adder-all.jar .
CMD java -jar build/adder-all.jar ${CONFIGPATH}
