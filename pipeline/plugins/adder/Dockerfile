FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/adder-all.jar .
CMD java -jar adder-all.jar ${CONFIGPATH}
