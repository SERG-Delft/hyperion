FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY build/stresser-all.jar .
CMD java -jar stresser-all.jar ${CONFIGPATH}
