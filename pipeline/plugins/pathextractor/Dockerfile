FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/pathextractor-all.jar .
CMD java -jar pathextractor-all.jar ${CONFIGPATH}
