FROM docker-io.dbc.dk/dbc-payara-logback:latest

USER root

COPY target/config $PAYARA_HOME/glassfish/domains/domain1/config
COPY target/lib/ext/* $PAYARA_HOME/glassfish/domains/domain1/lib/ext/

ARG as_admin_password
ENV AS_ADMIN_PASSWORD ${as_admin_password}

EXPOSE 4848
EXPOSE 8080
