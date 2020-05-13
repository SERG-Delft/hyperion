FROM openjdk:14
ARG configpath="pluginmanager.yaml"
ENV CONFIGPATH ${configpath}
COPY renamer-all.jar .
CMD java -jar renamer-all.jar ${CONFIGPATH}
