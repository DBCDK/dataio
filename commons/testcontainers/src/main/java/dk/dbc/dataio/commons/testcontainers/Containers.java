package dk.dbc.dataio.commons.testcontainers;

import org.testcontainers.containers.GenericContainer;

public enum Containers {
    FILE_STORE("dataio-file-store-service-war:" + getTag()),
    FLOW_STORE("dataio-flow-store-service:" + getTag()),
    JOB_STORE("dataio-job-store-service-war:" + getTag()),
    JMS_QUEUE_SVC("dataio-integration-test-jms-queue-service:" + getTag()),
    ARTEMIS("artemis:2_24_0-15"),
    LOG_STORE("dataio-log-store-service-war:" + getTag());

    private final String dockerRepo;
    private final String path;

    Containers(String path) {
        this("docker-metascrum.artifacts.dbccloud.dk", path);
    }

    Containers(String dockerRepo, String path) {
        this.dockerRepo = dockerRepo;
        this.path = path;
    }

    public GenericContainer<?> makeContainer() {
        return new GenericContainer<>(dockerRepo + "/" + path);
    }

    public static String getTag() {
        String tag = System.getProperty("tag");
        if (tag != null && !tag.isEmpty()) {
            return tag;
        } else {
            return "devel";
        }
    }
}
