/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.commons.testcontainers;

import org.testcontainers.containers.GenericContainer;

public class Containers {
    private Containers() {}

    public static GenericContainer filestoreServiceContainer() {
        return new GenericContainer(
                "docker-io.dbc.dk/dbc-payara-filestore:" + getTag());
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
