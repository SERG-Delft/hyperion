FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/rate-all.jar .
CMD java -jar rate-all.jar ${CONFIGPATH}
