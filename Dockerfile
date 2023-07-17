FROM docker.powerflow.cloud/base/openjdk:11

ENV APPLICATION_HOME=/app
ENV LOGS_PATH=${APPLICATION_HOME}/logs
ENV JAVA_OPTS=""

USER root

RUN mkdir -p ${LOGS_PATH}

ADD ./*.jar ${APPLICATION_HOME}/app.jar

RUN chown -R javauser:root ${APPLICATION_HOME} \
    && chmod -R 775 ${APPLICATION_HOME} 

WORKDIR /app/

EXPOSE 8080

USER 1001

ENTRYPOINT ["sh", "-c", "java `eval echo \"$JAVA_OPTS\"` -jar ${APPLICATION_HOME}/app.jar"]
