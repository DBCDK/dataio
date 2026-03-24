package dk.dbc.dataio.commons.testcontainers;

import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public enum Containers {
    FILE_STORE("file-store-service/target/docker.out"),
    FLOW_STORE("flow-store-service/target/docker.out"),
    JOB_STORE("job-store-service/war/target/docker.out"),
    ARTEMIS("docker-metascrum.artifacts.dbccloud.dk/artemis:2_30_0-0"),
    LOG_STORE("log-store-service/war/target/docker.out");

    private final String imageSource;

    Containers(String imageSource) {
        this.imageSource = imageSource;
    }

    public GenericContainer<?> makeContainer() {
        return new GenericContainer<>(resolveImageName());
    }

    private String resolveImageName() {
        if (this == ARTEMIS) {
            return imageSource;
        }

        Path projectRoot = getProjectRoot();
        Path dockerOut = projectRoot.resolve(imageSource);
        try {
            return Files.readString(dockerOut).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read docker image name from " + dockerOut, e);
        }
    }

    private Path getProjectRoot() {
        return Path.of(System.getProperty("maven.multiModuleProjectDirectory"));
    }
}