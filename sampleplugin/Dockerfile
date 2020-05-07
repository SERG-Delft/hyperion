FROM openjdk:14
ARG configpath="sampleplugin.yaml"
COPY sampleplugin-all.jar .
COPY sampleplugin.yaml .
ENV CONFIGPATH ${configpath}
CMD java -jar sampleplugin-all.jar ${CONFIGPATH}
