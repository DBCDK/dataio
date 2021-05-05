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

package dk.dbc.dataio.sink.es;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.sink.es.entity.inflight.EsInFlight;
import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSException;
import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.asString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class EsMessageProcessorBeanIT extends SinkIT {

    private EsSinkConfig esSinkConfig;
    private Sink sink;

    @Before
    public void setUpSink() {
        esSinkConfig = new EsSinkConfig().withUserId(3).withDatabaseName("dbname");
        SinkContent sinkContent = new SinkContentBuilder().setSinkConfig(esSinkConfig).build();

        sink = new SinkBuilder().setId(1).setContent(sinkContent).build();

        try {
            when(flowStoreServiceConnector.getSink(sink.getId())).thenReturn(sink);
        } catch (FlowStoreServiceConnectorException e) {
            throw new IllegalStateException("Error");
        }
    }

    @Test
    public void onMessage_chunkWithAllRecordsFailedOrIgnored_noTaskPackageInFlight()
            throws JSONBException, JMSException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {

        // Mock JobInfoSnapshot returned from jobStoreServiceConnector.addChunk() call
        jobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());

        // Create processor chunk with 10 failed items:
        final int itemsInChunk = 10;
        final List<ChunkItem> items = new ArrayList<>(itemsInChunk);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.IGNORE).build());
        for(long i=1; i<itemsInChunk; i++) {
            items.add(new ChunkItemBuilder().setId(i).setStatus(ChunkItem.Status.FAILURE).build());
        }
        final Chunk processorChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(items).build();
        final MockedJmsTextMessage sinkMessage = getSinkMessage(processorChunk, sink);

        final EsMessageProcessorBean esMessageProcessorBean = getEsMessageProcessorBean();
        esMessageProcessorBean.onMessage(sinkMessage);

        // Assert that no ES task packages are in-flight
        assertThat("Number of ES task packages in-flight", listEsInFlight().size(), is(0));

        // Assert that the ES sink config, sinkId and sinkVersion has been set correctly
        assertThat("highestVersionSeen", esMessageProcessorBean.highestVersionSeen, is(sink.getVersion()));
        assertThat("sinkConfig", esMessageProcessorBean.sinkConfig, is(esSinkConfig));
        assertThat("sinkId", esMessageProcessorBean.sinkId, is(Long.valueOf(System.getenv().get(Constants.SINK_ID_ENV_VARIABLE))));

        // Assert that sink chunk corresponds to processor chunk and that all items are ignored:
        final Chunk sinkChunk = jobStoreServiceConnector.chunks.remove();
        assertThat("chunk.getJobId()", sinkChunk.getJobId(), is(processorChunk.getJobId()));
        assertThat("chunk.getChunkkId()", sinkChunk.getChunkId(), is(processorChunk.getChunkId()));
        assertThat("chunk.size()", sinkChunk.size(), is(itemsInChunk));
        for (final ChunkItem chunkItem : sinkChunk) {
            assertThat("chunkItem.getStatus()", chunkItem.getStatus(), is(ChunkItem.Status.IGNORE));
        }
    }

    @Test
    public void onMessage_chunkWithValidAddi_createsTaskPackageAndInFlight() throws JSONBException, JMSException {

        final List<ChunkItem> items = new ArrayList<>(4);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.IGNORE).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.SUCCESS).setData(getValidAddi()).build());
        items.add(new ChunkItemBuilder().setId(2).setStatus(ChunkItem.Status.FAILURE).build());
        items.add(new ChunkItemBuilder().setId(3).setStatus(ChunkItem.Status.SUCCESS).setData(getValidAddiWithProcessingTrue()).build());
        final Chunk processorChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(items).build();
        final MockedJmsTextMessage sinkMessage = getSinkMessage(processorChunk, sink);

        final EsMessageProcessorBean esMessageProcessorBean = getEsMessageProcessorBean();
        final EntityTransaction transaction = esInFlightEntityManager.getTransaction();
        final EntityTransaction esTransaction = esMessageProcessorBean.esConnector.entityManager.getTransaction();
        transaction.begin();
        esTransaction.begin();
        esMessageProcessorBean.onMessage(sinkMessage);
        esTransaction.commit();
        transaction.commit();

        final List<EsInFlight> esInFlights = listEsInFlight();

        // Assert that one ES task packages is in-flight
        assertThat("Number of ES task packages in-flight", listEsInFlight().size(), is(1));

        final EsInFlight esInFlight = esInFlights.get(0);
        assertThat("esInFlight.sinkId", esInFlight.getSinkId(), is(sink.getId()));
        assertThat("esInFlight.databaseName", esInFlight.getDatabaseName(), is(esSinkConfig.getDatabaseName()));

        // Assert in-flight placeholder chunk content
        final Chunk placeholderChunk = jsonbContext.unmarshall(esInFlight.getIncompleteDeliveredChunk(), Chunk.class);
        ChunkItem next;
        final Iterator<ChunkItem> iterator = placeholderChunk.iterator();
        iterator.next();   // IGNORE item
        next = iterator.next();
        assertThat("Number of Addi records in second item", asString(next.getData()), is("1"));
        iterator.next();   // FAILURE item
        next = iterator.next();
        assertThat("Number of Addi records in fourth item", asString(next.getData()), is("1"));

        // Assert that one task package is created in ES
        final List<Integer> taskPackages = findTaskPackages();
        assertThat("Number of task packages in ES", taskPackages.size(), is(1));
        assertThat("ES target reference matches in-flight reference", taskPackages.get(0), is(esInFlight.getTargetReference()));
    }

    @Test
    public void onMessage_chunkWithValidAddiWithMultipleRecords_createsTaskPackageAndInFlight() throws JSONBException, JMSException {
        final List<ChunkItem> items = new ArrayList<>(4);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.IGNORE).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.SUCCESS).setData(getValidAddiWithMultipleRecords(3)).build());
        items.add(new ChunkItemBuilder().setId(2).setStatus(ChunkItem.Status.FAILURE).build());
        items.add(new ChunkItemBuilder().setId(3).setStatus(ChunkItem.Status.SUCCESS).setData(getValidAddiWithMultipleRecordsWithProcessingTrue(2)).build());
        final Chunk processorChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(items).build();
        final MockedJmsTextMessage sinkMessage = getSinkMessage(processorChunk);

        final EsMessageProcessorBean esMessageProcessorBean = getEsMessageProcessorBean();
        final EntityTransaction transaction = esInFlightEntityManager.getTransaction();
        final EntityTransaction esTransaction = esMessageProcessorBean.esConnector.entityManager.getTransaction();
        esTransaction.begin();
        transaction.begin();
        esMessageProcessorBean.onMessage(sinkMessage);
        transaction.commit();
        esTransaction.commit();

        final List<EsInFlight> esInFlights = listEsInFlight();

        // Assert that one ES task packages is in-flight
        assertThat("Number of ES task packages in-flight", listEsInFlight().size(), is(1));

        final EsInFlight esInFlight = esInFlights.get(0);

        // Assert in-flight placeholder chunk content
        final Chunk placeholderChunk = jsonbContext.unmarshall(esInFlight.getIncompleteDeliveredChunk(), Chunk.class);
        ChunkItem next;
        final Iterator<ChunkItem> iterator = placeholderChunk.iterator();
        iterator.next();   // IGNORE item
        next = iterator.next();
        assertThat("Number of Addi records in second item", asString(next.getData()), is("3"));
        iterator.next();   // FAILURE item
        next = iterator.next();
        assertThat("Number of Addi records in fourth item", asString(next.getData()), is("2"));

        // Assert that one task package is created in ES
        final List<Integer> taskPackages = findTaskPackages();
        assertThat("Number of task packages in ES", taskPackages.size(), is(1));
        assertThat("ES target reference matches in-flight reference", taskPackages.get(0), is(esInFlight.getTargetReference()));
    }
}
