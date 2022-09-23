package dk.dbc.dataio.commons.testcontainers;

import org.testcontainers.containers.GenericContainer;

public enum Containers {
    FILE_STORE("dbc-payara-filestore:" + getTag()),
    FLOW_STORE("dbc-payara-flowstore:" + getTag()),
    JOB_STORE("dbc-payara-jobstore:" + getTag()),
    JMS_QUEUE_SVC("dbc-payara-jms-queue-service:" + getTag()),
    ARTEMIS("artemis:2_24_0-15"),
    LOG_STORE("dbc-payara-logstore:" + getTag());

    private final String host;
    private final String path;

    Containers(String path) {
        this("docker-metascrum.artifacts.dbccloud.dk", path);
    }

    Containers(String host, String path) {
        this.host = host;
        this.path = path;
    }

    public GenericContainer<?> makeContainer() {
        return new GenericContainer<>(host + "/" + path);
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
