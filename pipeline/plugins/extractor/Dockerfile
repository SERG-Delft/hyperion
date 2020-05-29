FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/extractor-all.jar .
CMD java -jar extractor-all.jar ${CONFIGPATH}
