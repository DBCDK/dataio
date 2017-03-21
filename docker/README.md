# Docker 

## Build scripts

**TODO:** Add desription of build Scripts.

# Local Test for complete system

## Setup

Der skal være et netværk der heder dataio-dev 

```bash
docker network create --subnet 192.168.18.5/24 dataio-dev
```

## build -- update component 

```bash
mvn verify 
./docker/build-all-images
```

## Run 

```bash
docker-compose -f docker-compose-devel.yml up -d

```

Start en bash i docker netværket.
 
```bash
docker run --name=dataio-dev-tools --network-alias=dataio-dev-tools --network=dataio-dev --rm -ti -u $UID -v /etc/passwd:/etc/passwd -v /home/$USER:/home/$USER docker.dbc.dk/dbc-python2 /bin/bash

```

i den docker kan man så creare et job i mod sit kørende system
 
 ```bash
cd dataio/developer-tools
./create_job.py --filestorehost=filestore:8080 --jobstore=jobstore:8080  ./testdata/870970.2poster.iso ./testdata/870970.2poster.iso.spec
 ```

## Roadmap - mangler

  1. Test 
