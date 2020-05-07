FROM openjdk:14
ARG configpath=/pluginmanager
COPY pluginmanager-all.jar .
COPY pluginmanager.yaml .
CMD java -jar pluginmanager-all.jar $configpath
