FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/printer-all.jar .
CMD java -jar printer-all.jar ${CONFIGPATH}
