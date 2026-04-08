FROM docker-dbc.artifacts.dbccloud.dk/dbc-java${java.rt.version}:latest
LABEL org.opencontainers.image.authors="${team}"

ENV APP_NAME=${app-name} \
    LOG_FORMAT=json \
    LOG_LEVEL=INFO \
    TZ=Europe/Copenhagen

COPY --chown=java:java target/${project.build.finalName}.jar /home/java/app/

USER java
CMD ["/bin/sh", "-c", "java -Xms${INIT_HEAP:-128m} -Xmx${MAX_HEAP:-2g} -XX:+ExitOnOutOfMemoryError -Dlogback.configurationFile=logback-jobstore.xml -jar /home/java/app/${project.build.finalName}.jar"]