package dk.dbc.dataio.harvester.dmatdm3;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.MarcXchangeCollection;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.dmat.service.persistence.enums.Selection;
import dk.dbc.dmat.service.persistence.enums.UpdateCode;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordFetcherTest {
    private final RecordServiceConnector connector = mock(RecordServiceConnector.class);
    private final byte[] emptyCollection = new MarcXchangeCollection().emptyCollection();
    private final List<RecordEntryDTO> someRecordList = List.of(
            new RecordEntryDTO()
    );

    private static final String RECORD_ID = "12345678";
    private static final String REVIEW_ID = "87654321";
    private static final String MATCH_ID = "11223344";

    private RecordServiceConnector.Params useParentAgencyParam() {
        return new RecordServiceConnector.Params()
                .withUseParentAgency(true);
    }

    @Test
    void newCreateReturnsEmptyCollection() throws RecordServiceConnectorException, HarvesterException, JsonProcessingException {
        DMatRecord dMatRecord = new DMatRecord().withUpdateCode(UpdateCode.NEW).withSelection(Selection.CREATE);

        byte[] result = RecordFetcher.getRecordCollectionFor(connector, dMatRecord);

        assertThat(result, is(emptyCollection));
        verify(connector, never()).getRecordDataCollection(anyInt(), anyString(), any());
    }

    @Test
    void autoCreateReturnsEmptyCollection() throws RecordServiceConnectorException, HarvesterException, JsonProcessingException {
        DMatRecord dMatRecord = new DMatRecord().withUpdateCode(UpdateCode.AUTO).withSelection(Selection.CREATE);

        byte[] result = RecordFetcher.getRecordCollectionFor(connector, dMatRecord);

        assertThat(result, is(emptyCollection));
        verify(connector, never()).getRecordDataCollection(anyInt(), anyString(), any());
    }

    @Test
    void newCloneFetchesMatchRecord() throws RecordServiceConnectorException, HarvesterException, JsonProcessingException {
        DMatRecord dMatRecord = new DMatRecord().withUpdateCode(UpdateCode.NEW).withSelection(Selection.CLONE).withMatch(MATCH_ID);
        when(connector.getRecordDataCollection(191919, MATCH_ID, useParentAgencyParam())).thenReturn(someRecordList);

        RecordFetcher.getRecordCollectionFor(connector, dMatRecord);

        verify(connector).getRecordDataCollection(191919, MATCH_ID, useParentAgencyParam());
    }

    @Test
    void autoCloneFetchesMatchRecord() throws RecordServiceConnectorException, HarvesterException, JsonProcessingException {
        DMatRecord dMatRecord = new DMatRecord().withUpdateCode(UpdateCode.AUTO).withSelection(Selection.CLONE).withMatch(MATCH_ID);
        when(connector.getRecordDataCollection(191919, MATCH_ID, useParentAgencyParam())).thenReturn(someRecordList);

        RecordFetcher.getRecordCollectionFor(connector, dMatRecord);

        verify(connector).getRecordDataCollection(191919, MATCH_ID, useParentAgencyParam());
    }

    @Test
    void actCreateReturnsEmptyCollection() throws RecordServiceConnectorException, HarvesterException, JsonProcessingException {
        DMatRecord dMatRecord = new DMatRecord().withUpdateCode(UpdateCode.ACT).withSelection(Selection.CREATE);

        byte[] result = RecordFetcher.getRecordCollectionFor(connector, dMatRecord);

        assertThat(result, is(emptyCollection));
        verify(connector, never()).getRecordDataCollection(anyInt(), anyString(), any());
    }

    @Test
    void nnbDropReturnsEmptyCollection() throws RecordServiceConnectorException, HarvesterException, JsonProcessingException {
        DMatRecord dMatRecord = new DMatRecord().withUpdateCode(UpdateCode.NNB).withSelection(Selection.DROP);

        byte[] result = RecordFetcher.getRecordCollectionFor(connector, dMatRecord);

        assertThat(result, is(emptyCollection));
        verify(connector, never()).getRecordDataCollection(anyInt(), anyString(), any());
    }

    @Test
    void nnbAutodropReturnsEmptyCollection() throws RecordServiceConnectorException, HarvesterException, JsonProcessingException {
        DMatRecord dMatRecord = new DMatRecord().withUpdateCode(UpdateCode.NNB).withSelection(Selection.AUTODROP);

        byte[] result = RecordFetcher.getRecordCollectionFor(connector, dMatRecord);

        assertThat(result, is(emptyCollection));
        verify(connector, never()).getRecordDataCollection(anyInt(), anyString(), any());
    }

    @Test
    void reviewFetchesReviewIdRecord() throws RecordServiceConnectorException, HarvesterException, JsonProcessingException {
        DMatRecord dMatRecord = new DMatRecord().withUpdateCode(UpdateCode.REVIEW).withSelection(Selection.CREATE).withReviewId(REVIEW_ID);
        when(connector.getRecordDataCollection(191919, REVIEW_ID, useParentAgencyParam())).thenReturn(someRecordList);

        RecordFetcher.getRecordCollectionFor(connector, dMatRecord);

        verify(connector).getRecordDataCollection(191919, REVIEW_ID, useParentAgencyParam());
    }

    @Test
    void updateFetchesOwnRecord() throws RecordServiceConnectorException, HarvesterException, JsonProcessingException {
        DMatRecord dMatRecord = new DMatRecord().withUpdateCode(UpdateCode.UPDATE).withSelection(Selection.CREATE).withRecordId(RECORD_ID);
        when(connector.getRecordDataCollection(191919, RECORD_ID, useParentAgencyParam())).thenReturn(someRecordList);

        RecordFetcher.getRecordCollectionFor(connector, dMatRecord);

        verify(connector).getRecordDataCollection(191919, RECORD_ID, useParentAgencyParam());
    }

    @Test
    void invalidCombinationThrowsHarvesterException() {
        DMatRecord dMatRecord = new DMatRecord().withUpdateCode(UpdateCode.ACT).withSelection(Selection.CLONE);

        assertThrows(HarvesterException.class, () -> RecordFetcher.getRecordCollectionFor(connector, dMatRecord));
    }
}
