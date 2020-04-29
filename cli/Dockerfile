FROM docker.dbc.dk/dbc-java8

ENV JARDIR /usr/bin

COPY acceptance-test/target/dataio-cli-acctest.jar $JARDIR
COPY datafile-exporter/target/dataio-cli-datafile-exporter.jar $JARDIR
COPY job-replicator/target/dataio-cli-job-replicator.jar $JARDIR
COPY lhr-retriever/target/dataio-cli-lhr-retriever.jar $JARDIR
