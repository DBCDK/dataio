FROM docker-dbc.artifacts.dbccloud.dk/dbc-java17:latest
LABEL org.opencontainers.image.authors="de-team"

ENV LOG_FORMAT=json \
    LOG_LEVEL=INFO \
    TZ=Europe/Copenhagen

USER root
RUN apt-get update -o Acquire::AllowInsecureRepositories=true \
        -o Acquire::AllowDowngradeToInsecureRepositories=true \
    && apt-get install -qy --no-install-recommends libxml2-utils \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY --chown=java:java script/jsondiff /home/java/tools/
COPY --chown=java:java script/plaintextdiff /home/java/tools/
COPY --chown=java:java script/xmldiff /home/java/tools/
RUN chmod +x /home/java/tools/*

COPY --chown=java:java target/${project.build.finalName}.jar /home/java/app/

USER java
CMD ["/bin/sh", "-c", "java -Xms${INIT_HEAP:-128m} -Xmx${MAX_HEAP:-256m} -XX:+ExitOnOutOfMemoryError -jar /home/java/app/${project.build.finalName}.jar"]