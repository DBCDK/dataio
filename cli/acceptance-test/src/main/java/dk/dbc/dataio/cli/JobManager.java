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

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.utils.JunitXmlStreamWriter;
import dk.dbc.dataio.commons.utils.JunitXmlTestCase;
import dk.dbc.dataio.commons.utils.JunitXmlTestSuite;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.PrettyPrint;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.AccTestJobInputStream;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Class managing all interactions with the dataIO job-store needed for acceptance test operation
 */
public class JobManager {

    private static final String JUNIT_XML = "JUnit.xml";
    private static final long SLEEP_INTERVAL_IN_MS = 10000;     // 10 minutes
    private static final long MAX_WAIT_IN_MS       = 28800000;  // 8 hours
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final FileStoreServiceConnector fileStoreServiceConnector;

    public JobManager(String jobStoreEndpoint, String fileStoreEndPoint) {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        jobStoreServiceConnector = new JobStoreServiceConnector(client, jobStoreEndpoint);
        fileStoreServiceConnector = initializeFileStoreServiceConnector(fileStoreEndPoint);
    }

    public JobInfoSnapshot addAccTestJob(TestSuite testSuite, Flow flow) throws Exception {
        final String fileId = addDataFile(testSuite.getDataFile());
        final JobSpecification jobSpecification = createJobSpecification(testSuite.getProperties(), fileId);
        final AccTestJobInputStream jobInputStream = new AccTestJobInputStream(
                jobSpecification,
                flow,
                RecordSplitterConstants.RecordSplitter.valueOf((String) testSuite.getProperties().get("recordSplitter")));

        final JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addAccTestJob(jobInputStream);
        return waitForJobCompletion(jobInfoSnapshot.getJobId(), testSuite);
    }

    public static int failedItems(State state) {
        return state.getPhase(State.Phase.PARTITIONING).getFailed() +
                state.getPhase(State.Phase.PROCESSING).getFailed() +
                state.getPhase(State.Phase.DELIVERING).getFailed();
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

    private JobSpecification createJobSpecification(Properties jobProperties, String fileId) {
        final FileStoreUrn fileStoreUrn = FileStoreUrn.create(fileId);
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

    private JobInfoSnapshot waitForJobCompletion(long jobId, TestSuite testSuite) throws Exception {
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
        createAccTestResult(jobInfoSnapshot, testSuite);
        return jobInfoSnapshot;
    }

    private void createAccTestResult(JobInfoSnapshot jobInfoSnapshot, TestSuite testSuite) throws Exception {
        try (FileOutputStream fileOutputStream = new FileOutputStream(testSuite.getName() + JUNIT_XML)) {
            createJunitXmlTestSuite(fileOutputStream, jobInfoSnapshot, testSuite.getName());
        }
    }

    private void createJunitXmlTestSuite(FileOutputStream fileOutputStream, JobInfoSnapshot jobInfoSnapshot, String testSuiteName) throws Exception {
        try (final JunitXmlStreamWriter junitXmlStreamWriter = new JunitXmlStreamWriter(fileOutputStream)) {
            try (final JunitXmlTestSuite junitXmlTestSuite = new JunitXmlTestSuite("dataio.acctest." + testSuiteName, junitXmlStreamWriter)) {
                createJunitXmlTestCases(junitXmlTestSuite, jobInfoSnapshot, testSuiteName);
            }
        }
    }

    private void createJunitXmlTestCases(JunitXmlTestSuite junitXmlTestSuite, JobInfoSnapshot jobInfoSnapshot, String testSuiteName) throws JobStoreServiceConnectorException, XMLStreamException {
        int itemsAddedToTestSuite = 0;
        int chunkId = 0;
        if (failedItems(jobInfoSnapshot.getState()) > 0) {
            while (itemsAddedToTestSuite < jobInfoSnapshot.getNumberOfItems()) {
                int itemId = 0;
                final Map<Integer, ItemInfoSnapshot> failedSnapshots = getFailedItemInfoSnapshots(jobInfoSnapshot.getJobId(), chunkId);
                while (itemId < 10 && itemsAddedToTestSuite < jobInfoSnapshot.getNumberOfItems()) {
                    if (failedSnapshots.containsKey(itemId)) {
                        junitXmlTestSuite.addTestCase(getFailedTestCase(failedSnapshots.get(itemId), testSuiteName, jobInfoSnapshot.getJobId(), ++itemsAddedToTestSuite));
                    } else {
                        junitXmlTestSuite.addTestCase(getPassedTestCase(testSuiteName, jobInfoSnapshot.getJobId(), ++itemsAddedToTestSuite));
                    }
                    itemId++;
                }
                chunkId++;
            }
        } else {
            while (itemsAddedToTestSuite < jobInfoSnapshot.getNumberOfItems()) {
                junitXmlTestSuite.addTestCase(getPassedTestCase(testSuiteName, jobInfoSnapshot.getJobId(), ++itemsAddedToTestSuite));
            }
        }
    }

    private Map<Integer, ItemInfoSnapshot> getFailedItemInfoSnapshots(int jobId, int chunkId) throws JobStoreServiceConnectorException {
        final ItemListCriteria itemListCriteria = getItemListCriteria(jobId, chunkId);
        return jobStoreServiceConnector.listItems(itemListCriteria).stream()
                .collect(Collectors.toMap(c -> (int) c.getItemId(), c -> c));
    }

    private ItemListCriteria getItemListCriteria(int jobId, int chunkId) {
        return new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId))
                .and(new ListFilter<>(ItemListCriteria.Field.CHUNK_ID, ListFilter.Op.EQUAL, chunkId))
                .and(new ListFilter<>(ItemListCriteria.Field.STATE_FAILED))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC));
    }

    private JunitXmlTestCase getFailedTestCase(ItemInfoSnapshot itemInfoSnapshot, String testSuiteName, int jobId, int recordNumber)
            throws XMLStreamException, JobStoreServiceConnectorException {

        final ChunkItem chunkItem = jobStoreServiceConnector.getChunkItem(
                itemInfoSnapshot.getJobId(),
                itemInfoSnapshot.getChunkId(),
                itemInfoSnapshot.getItemId() ,
                State.Phase.DELIVERING);
        final String recordId = itemInfoSnapshot.getRecordInfo()!=null?itemInfoSnapshot.getRecordInfo().getId():"";

        // The fixed length string is used to make the ascending listing of items correct in jenkins
        return JunitXmlTestCase.failed(
                String.format("job %d, post %s, record %s", jobId, String.format("%1$5d", recordNumber), recordId),
                String.format("dataio.acctest.%s.failed", testSuiteName),
                String.format("Failed for post %d with record id %s", recordNumber, recordId),
                PrettyPrint.asXml(chunkItem.getData(), chunkItem.getEncoding()));
    }

    private JunitXmlTestCase getPassedTestCase(String testSuiteName, int jobId, int recordNumber) {
        // The fixed length string is used to make the ascending listing of items correct in jenkins
        return JunitXmlTestCase.passed(
                String.format("job %d, post %s", jobId, String.format("%1$5d", recordNumber)),
                String.format("dataio.acctest.%s.passed", testSuiteName));
    }
}