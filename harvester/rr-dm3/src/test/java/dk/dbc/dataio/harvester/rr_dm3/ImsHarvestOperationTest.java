package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
public class ImsHarvestOperationTest extends HarvestOperationTest {
    private final static Set<Integer> IMS_LIBRARIES = Set.of(710100, 737000, 775100, 785100);
    private final VipCoreConnection vipCoreConnection = mock(VipCoreConnection.class);
    private final HoldingsItemsConnector holdingsItemsConnector = mock(HoldingsItemsConnector.class);

    public static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Timer timer = mock(Timer.class);
    private final Counter counter = mock(Counter.class);

    @BeforeEach
    public void setupImsHarvestOperationTestMocks() throws HarvesterException {
        when(vipCoreConnection.getFbsImsLibraries()).thenReturn(IMS_LIBRARIES);
        when(metricRegistry.timer(any(Metadata.class), any(Tag.class))).thenReturn(timer);
        when(metricRegistry.counter(any(Metadata.class), any(Tag.class))).thenReturn(counter);
        doNothing().when(timer).update(any(Duration.class));
        doNothing().when(counter).inc();
    }

    @Test
    public void execute_harvestedRecordHasNonDbcAndNonImsAgencyId_recordIsSkipped()
            throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        QueueItem queueItem = getQueueItem(
                new RecordIdDTO("bibliographicRecordId", 123456));

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoRecordServiceConnector, times(0)).getRecordData(any(RecordIdDTO.class));
    }

    @Test
    public void execute_harvestedRecordHasImsAgencyId_recordIsProcessed()
            throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        QueueItem queueItem = getQueueItem(
                new RecordIdDTO("bibliographicRecordId", 710100));

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoRecordServiceConnector, times(2)).getRecordData(any(RecordIdDTO.class));
    }

    @Test
    public void execute_harvestedRecordHasDbcAgencyId_recordIsProcessed()
            throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        QueueItem queueItem = getQueueItem(
                new RecordIdDTO("bibliographicRecordId", 710100));

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        RecordEntryDTO record = new RecordEntryBuilder().id(DBC_RECORD_ID).createdNow().deleteContent('e').deleted().build();

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class)))
                .thenReturn(List.of(record));

        when(rawRepoRecordServiceConnector.getRecordData(any(RecordIdDTO.class))).thenReturn(record);

        Set<Integer> set = new HashSet<>();
        set.add(1);

        when(holdingsItemsConnector.hasHoldings(anyString(), anySet())).thenReturn(set);
        when(rawRepoRecordServiceConnector.recordExists(anyInt(), anyString())).thenReturn(true);

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoRecordServiceConnector, times(2)).getRecordData(any(RecordIdDTO.class));
    }

    @Test
    public void execute_harvestedRecordHasDbcAgencyIdAndHoldingsItemsLookupReturnsImsAgencyIds_recordIsProcessed()
            throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        QueueItem queueItem = getQueueItem(DBC_RECORD_ID);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        when(holdingsItemsConnector.hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES))
                .thenReturn(IMS_LIBRARIES);

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(holdingsItemsConnector, times(1)).hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES);
        verify(rawRepoRecordServiceConnector, times(8)).getRecordData(any(RecordIdDTO.class));
    }

    @Test
    public void execute_harvestedRecordHasDbcAgencyIdAndHoldingsItemsLookupReturnsNonImsAgencyIds_recordIsSkipped()
            throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        QueueItem queueItem = getQueueItem(DBC_RECORD_ID);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        when(holdingsItemsConnector.hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES))
                .thenReturn(new HashSet<>(Collections.singletonList(123456)));

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(holdingsItemsConnector, times(1)).hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES);
        verify(rawRepoRecordServiceConnector, times(0)).getRecordData(any(RecordIdDTO.class));
    }

    @Override
    public void execute_rawRepoDeleteRecordHasAgencyIdContainedInExcludedSet_recordIsProcessed() {
        // Irrelevant test from super class
    }

    @Override
    public void execute_rawRepoDeleteRecordHasDbcId_recordIsSkipped() {
        // Irrelevant test from super class
    }

    @Override
    public HarvestOperation newHarvestOperation() {
        return newHarvestOperation(HarvesterTestUtil.getRRHarvesterConfig());
    }

    @Override
    public HarvestOperation newHarvestOperation(RRHarvesterConfig config) {
        try {
            return new ImsHarvestOperation(config, harvesterJobBuilderFactory, taskRepo,
                    vipCoreConnection, rawRepoConnector, holdingsItemsConnector,
                    rawRepoRecordServiceConnector, metricRegistry);
        } catch (QueueException | SQLException | ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}
