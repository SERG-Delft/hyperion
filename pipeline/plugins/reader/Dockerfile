FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/reader-all.jar .
CMD java -jar build/reader-all.jar ${CONFIGPATH}
