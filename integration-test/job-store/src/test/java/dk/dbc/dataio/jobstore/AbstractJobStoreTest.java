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

package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.After;
import org.junit.BeforeClass;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/* NOTES ABOUT DISABLED TESTS

   These tests should be migrated towards using a testcontainers approach.

   This requires running both jobstore, filestore, flowstore, openmq and jms-queue-service
   containers which entails an amount of work which is deemed out of scope of the current
   feature. For the immediate future tracerbullet tests will have to suffice.
*/

public abstract class AbstractJobStoreTest {
    protected static FileStoreServiceConnector fileStoreServiceConnector;
    protected static FlowStoreServiceConnector flowStoreServiceConnector;
    protected static JobStoreServiceConnector jobStoreServiceConnector;

    @BeforeClass
    public static void setupClass() throws ClassNotFoundException {
        final Client httpClient = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));

        fileStoreServiceConnector = new FileStoreServiceConnector(httpClient, "tbd");
        flowStoreServiceConnector = new FlowStoreServiceConnector(httpClient, "tbd");
        jobStoreServiceConnector = new JobStoreServiceConnector(httpClient, "tbd");
    }

    @After
    public void emptyQueues() {
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);
    }

    public String createLineFormatDataFile() {
        try (final InputStream is = readTestRecord("/test-records-danmarc2.lin")) {
            return fileStoreServiceConnector.addFile(is);
        } catch (IOException | FileStoreServiceConnectorException e) {
            throw new IllegalStateException(e);
        }
    }

    private static InputStream readTestRecord(String resourceName) {
        return AbstractJobStoreTest.class.getResourceAsStream(resourceName);
    }

    protected void createFlowStoreEnvironmentMatchingJobSpecification(JobSpecification jobSpecification) {
        try {
            final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
            final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                    .setName(jobSpecification.getDestination())
                    .build());
            final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                    .setNumber(jobSpecification.getSubmitterId())
                    .build());
            flowStoreServiceConnector.createFlowBinder(new FlowBinderContentBuilder()
                    .setPackaging(jobSpecification.getPackaging())
                    .setFormat(jobSpecification.getFormat())
                    .setCharset(jobSpecification.getCharset())
                    .setDestination(jobSpecification.getDestination())
                    .setSubmitterIds(Collections.singletonList(submitter.getId()))
                    .setFlowId(flow.getId())
                    .setSinkId(sink.getId())
                    .setRecordSplitter(RecordSplitterConstants.RecordSplitter.DANMARC2_LINE_FORMAT)
                    .build());
        } catch (FlowStoreServiceConnectorException e) {
            throw new IllegalStateException(e);
        }
    }

    protected JobInputStream getJobInputStream(JobSpecification jobSpecification) {
        return new JobInputStream(jobSpecification, true, 0);
    }
}
