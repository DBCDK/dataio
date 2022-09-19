## ARTEMIS BASE IMAGE
This image is to be used throughout as base image for sinks, jobstore and jobprocessors.
Only these three queues are configured (and they are hardwired):
```
sinks
processor
dmq
```

So this image is NOT suited for other purposes than that of dataio.
The location of the artemis host must be configured in the env var `ARTEMIS_HOST` at startup.
