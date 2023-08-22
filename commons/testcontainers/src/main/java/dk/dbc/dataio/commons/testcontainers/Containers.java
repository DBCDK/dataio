package dk.dbc.dataio.commons.testcontainers;

import org.testcontainers.containers.GenericContainer;

public enum Containers {
    FILE_STORE("dataio-file-store-service-war:" + getTag()),
    FLOW_STORE("dataio-flow-store-service:" + getTag()),
    JOB_STORE("dbc-payara-jobstore:" + getTag()),
    JMS_QUEUE_SVC("dbc-payara-jms-queue-service:" + getTag()),
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
        String tag;
        final String buildNumber = System.getenv("BUILD_NUMBER");
        if (buildNumber == null || buildNumber.isEmpty()) {
            tag = "devel";
        } else {
            tag = buildNumber;
            final String branchName = System.getenv("BRANCH_NAME");
            if (branchName != null && !branchName.isEmpty()) {
                tag = branchName + "-" + buildNumber;
            }
        }
        return tag;
    }
}
