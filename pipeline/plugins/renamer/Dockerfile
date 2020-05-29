FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/renamer-all.jar .
CMD java -jar renamer-all.jar ${CONFIGPATH}
