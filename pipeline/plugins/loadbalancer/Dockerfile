FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/loadbalancer-all.jar .
CMD java -jar loadbalancer-all.jar ${CONFIGPATH}
