FROM docker.dbc.dk/dbc-postgres

USER root
RUN apt-get update
RUN apt-get -y install iproute2

USER $POSTGRES_LINUX_USER

ARG user
ENV POSTGRES_USER ${user}
ENV POSTGRES_DB ${user}
ENV POSTGRES_PASSWORD ${user}
