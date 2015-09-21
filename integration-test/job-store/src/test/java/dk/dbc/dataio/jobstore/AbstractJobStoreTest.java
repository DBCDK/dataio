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
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterXmlDataFile;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public abstract class AbstractJobStoreTest {
    protected static FileStoreServiceConnector fileStoreServiceConnector;
    protected static FlowStoreServiceConnector flowStoreServiceConnector;
    protected static JobStoreServiceConnector jobStoreServiceConnector;

    @BeforeClass
    public static void setupClass() throws ClassNotFoundException {
        final Client httpClient = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));

        fileStoreServiceConnector = new FileStoreServiceConnector(httpClient, ITUtil.FILE_STORE_BASE_URL);
        flowStoreServiceConnector = new FlowStoreServiceConnector(httpClient, ITUtil.FLOW_STORE_BASE_URL);
        jobStoreServiceConnector = new JobStoreServiceConnector(httpClient, ITUtil.JOB_STORE_BASE_URL);
    }

    @AfterClass
    public static void clearFileStore() {
        ITUtil.clearFileStore();
    }

    @After
    public void emptyQueues() {
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);
    }

    @After
    public void clearFlowStore() {
        ITUtil.clearFlowStore();
    }


    protected String createMarcxchangeHarvesterDataFile(File datafile, int numberOfRecords) {
        try (final OutputStream os = new FileOutputStream(datafile)) {
            try (final HarvesterXmlDataFile harvesterXmlDataFile = new HarvesterXmlDataFile(StandardCharsets.UTF_8, os)) {
                while (numberOfRecords-- > 0) {
                    final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection();
                    marcExchangeCollection.addMember(
                            "<marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\"/>".getBytes(StandardCharsets.UTF_8)
                    );
                    harvesterXmlDataFile.addRecord(marcExchangeCollection);
                }
            }
            try (final InputStream is = new FileInputStream(datafile)) {
                return fileStoreServiceConnector.addFile(is);
            }
        } catch (IOException | HarvesterException | FileStoreServiceConnectorException e) {
            throw new IllegalStateException(e);
        }
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
                    .setSubmitterIds(Arrays.asList(submitter.getId()))
                    .setFlowId(flow.getId())
                    .setSinkId(sink.getId())
                    .build());
        } catch (FlowStoreServiceConnectorException e) {
            throw new IllegalStateException(e);
        }
    }

    protected JobInputStream getJobInputStream(JobSpecification jobSpecification) {
        return new JobInputStream(jobSpecification, true, 0);
    }
}
