FROM openjdk:14
ARG configpath="pluginmanager.yml"
COPY build/pluginmanager-all.jar .
ENV CONFIGPATH ${configpath}
CMD java -jar pluginmanager-all.jar ${CONFIGPATH}
