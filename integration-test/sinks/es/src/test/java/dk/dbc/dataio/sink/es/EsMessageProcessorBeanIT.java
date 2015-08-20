package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import org.junit.Test;

import javax.jms.JMSException;
import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EsMessageProcessorBeanIT extends SinkIT {
    @Test
    public void onMessage_chunkWithAllRecordsFailedOrIgnored_noTaskPackageInFlight()
            throws JSONBException, JMSException, JobStoreServiceConnectorException {
        // Mock JobInfoSnapshot returned from jobStoreServiceConnector.addChunk() call
        jobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());

        // Create processor chunk with 10 failed items:
        final int itemsInChunk = 10;
        final List<ChunkItem> items = new ArrayList<>(itemsInChunk);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.IGNORE).build());
        for(long i=1; i<itemsInChunk; i++) {
            items.add(new ChunkItemBuilder().setId(i).setStatus(ChunkItem.Status.FAILURE).build());
        }
        final ExternalChunk processorChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(items).build();
        final MockedJmsTextMessage sinkMessage = getSinkMessage(processorChunk);

        final EsMessageProcessorBean esMessageProcessorBean = getEsMessageProcessorBean();
        esMessageProcessorBean.onMessage(sinkMessage);

        // Assert that no ES task packages are in-flight
        assertThat("Number of ES task packages in-flight", listEsInFlight().size(), is(0));

        // Assert that sink chunk corresponds to processor chunk and that all items are ignored:
        final ExternalChunk sinkChunk = jobStoreServiceConnector.chunks.remove();
        assertThat("chunk.getJobId()", sinkChunk.getJobId(), is(processorChunk.getJobId()));
        assertThat("chunk.getChunkkId()", sinkChunk.getChunkId(), is(processorChunk.getChunkId()));
        assertThat("chunk.size()", sinkChunk.size(), is(itemsInChunk));
        for (final ChunkItem chunkItem : sinkChunk) {
            assertThat("chunkItem.getStatus()", chunkItem.getStatus(), is(ChunkItem.Status.IGNORE));
        }
    }

    @Test
    public void onMessage_chunkWithValidAddi_createsTaskPackageAndInFlight() throws JSONBException, JMSException {
        // Create processor chunk with 10 failed items:
        final List<ChunkItem> items = new ArrayList<>(4);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.IGNORE).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.SUCCESS).setData(getValidAddi()).build());
        items.add(new ChunkItemBuilder().setId(2).setStatus(ChunkItem.Status.FAILURE).build());
        items.add(new ChunkItemBuilder().setId(3).setStatus(ChunkItem.Status.SUCCESS).setData(getValidAddiWithProcessingTrue()).build());
        final ExternalChunk processorChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(items).build();
        final MockedJmsTextMessage sinkMessage = getSinkMessage(processorChunk);

        final EsMessageProcessorBean esMessageProcessorBean = getEsMessageProcessorBean();
        final EntityTransaction transaction = esInFlightEntityManager.getTransaction();
        transaction.begin();
        esMessageProcessorBean.onMessage(sinkMessage);
        transaction.commit();

        final List<EsInFlight> esInFlights = listEsInFlight();

        // Assert that one ES task packages is in-flight
        assertThat("Number of ES task packages in-flight", listEsInFlight().size(), is(1));

        final EsInFlight esInFlight = esInFlights.get(0);
        assertThat("Number of record slots occupied by task package", esInFlight.getRecordSlots(), is(2));

        // Assert that one task package is created in ES
        final List<Integer> taskPackages = findTaskPackages();
        assertThat("Number of task packages in ES", taskPackages.size(), is(1));
        assertThat("ES target reference matches in-flight reference", taskPackages.get(0), is(esInFlight.getTargetReference()));
    }
}
