FROM docker.dbc.dk/dbc-java8

USER root

ENV CLIUSER dataio
ENV JARDIR /home/$CLIUSER/jars/

RUN useradd -m -s /bin/bash $CLIUSER
RUN mkdir $JARDIR

COPY acceptance-test/target/dataio-cli-acctest.jar $JARDIR
COPY job-replicator/target/dataio-cli-job-replicator.jar $JARDIR
COPY lhr-retriever/target/dataio-cli-lhr-retriever.jar $JARDIR

USER $CLIUSER
WORKDIR /home/$CLIUSER
