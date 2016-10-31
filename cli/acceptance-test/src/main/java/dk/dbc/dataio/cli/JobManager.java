/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.cli;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.AccTestJobInputStream;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Class managing all interactions with the dataIO job-store needed for acceptance test operation
 */
public class JobManager {

    private static final long SLEEP_INTERVAL_IN_MS = 10000; // 10 minutes
    private static final long MAX_WAIT_IN_MS = 28800000;    // 8 hours
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final FileStoreServiceConnector fileStoreServiceConnector;

    public JobManager(String jobStoreEndpoint, String fileStoreEndPoint) {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        jobStoreServiceConnector = new JobStoreServiceConnector(client, jobStoreEndpoint);
        fileStoreServiceConnector = initializeFileStoreServiceConnector(fileStoreEndPoint);
    }

    public JobInfoSnapshot addAccTestJob(TestSuite testSuite, Flow flow) throws JobStoreServiceConnectorException, FileStoreServiceConnectorException, IOException {
        final String fileId = addDataFile(testSuite.getDataFile());
        final JobSpecification jobSpecification = createJobSpecification(testSuite.getProperties(), fileId);
        final AccTestJobInputStream jobInputStream = new AccTestJobInputStream(
                jobSpecification,
                flow,
                RecordSplitterConstants.RecordSplitter.valueOf((String) testSuite.getProperties().get("recordSplitter")));

        final JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addAccTestJob(jobInputStream);
        return waitForJobCompletion(jobInfoSnapshot.getJobId());
    }

    public JobSpecification createJobSpecification(Properties jobProperties, String fileId) {
        final FileStoreUrn fileStoreUrn;
        try {
            fileStoreUrn = FileStoreUrn.create(fileId);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to create FileStoreUrn", e);
        }
        return new JobSpecification(
                "undefined",
                (String) jobProperties.get("format"),
                (String) jobProperties.get("charset"),
                "undefined",
                Long.parseLong((String)jobProperties.get("submitterId")),
                JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION,
                JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING,
                JobSpecification.EMPTY_RESULT_MAIL_INITIALS,
                fileStoreUrn.toString(),
                JobSpecification.Type.ACCTEST);
    }

    /*
     * Private methods
     */

    private FileStoreServiceConnector initializeFileStoreServiceConnector(String fileStoreEndpoint) {
        final ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        config.property(ApacheClientProperties.CONNECTION_MANAGER, new PoolingHttpClientConnectionManager());
        Client client = HttpClient.newClient(config);
        return new FileStoreServiceConnector(client, fileStoreEndpoint);
    }

    private String addDataFile(Path dataFile) throws FileStoreServiceConnectorException, IOException {
        try (final InputStream is = new FileInputStream(dataFile.toFile())) {
            return fileStoreServiceConnector.addFile(is);
        }
    }

    private JobInfoSnapshot waitForJobCompletion(long jobId) throws JobStoreServiceConnectorException {
        final JobListCriteria criteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        JobInfoSnapshot jobInfoSnapshot = null;
        // Wait for Job-completion
        long remainingWaitInMs = MAX_WAIT_IN_MS;

        while (remainingWaitInMs > 0) {
            jobInfoSnapshot = jobStoreServiceConnector.listJobs(criteria).get(0);
            if (jobInfoSnapshot.getTimeOfCompletion() != null) {
                break;
            } else {
                try {
                    Thread.sleep(SLEEP_INTERVAL_IN_MS);
                    remainingWaitInMs -= SLEEP_INTERVAL_IN_MS;
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        if (jobInfoSnapshot.getTimeOfCompletion() == null) {
            throw new IllegalStateException(String.format("Job %d did not complete in time",
                    jobInfoSnapshot.getJobId()));
        }
        return jobInfoSnapshot;
    }
}
