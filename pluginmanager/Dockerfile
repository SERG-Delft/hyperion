FROM openjdk:14
ARG configpath="pluginmanager.yaml"
COPY pluginmanager-all.jar .
ENV CONFIGPATH ${configpath}
CMD java -jar pluginmanager-all.jar ${CONFIGPATH}
