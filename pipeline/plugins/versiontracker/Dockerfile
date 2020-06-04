FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/versiontracker-all.jar .
CMD java -jar versiontracker-all.jar ${CONFIGPATH}
