FROM docker-dbc.artifacts.dbccloud.dk/payara6-micro:${docker.payara.version}
LABEL org.opencontainers.image.authors="${team}"

# TAG_STACK_ENABLED: whether HarvestOperation enriches harvested articles with tag suggestions
# from the tag-stack service.  Turned off while tag-stack is not in use.  The connector itself is
# still injected and TAG_STACK_SERVICE_URL is still required, so turning this back on needs no
# rebuild, only a deployment override.
ENV APP_NAME=${app-name} \
    TZ=Europe/Copenhagen \
    TAG_STACK_ENABLED=false

ADD target/*.war target/docker/*.json deployments/
