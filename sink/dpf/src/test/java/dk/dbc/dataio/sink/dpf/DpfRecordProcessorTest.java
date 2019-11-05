/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.ProcessingInstructions;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnectorException;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.IS_DOUBLE_RECORD;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.PROCESS_AS_NEW;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.SENT_TO_DOUBLE_RECORD_CHECK;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.SENT_TO_LOBBY;
import static dk.dbc.marc.binding.MarcRecord.hasSubFieldValue;
import static dk.dbc.marc.binding.MarcRecord.hasTag;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DpfRecordProcessorTest {
    private final ServiceBroker serviceBroker = mock(ServiceBroker.class);
    private final DpfRecordProcessor dpfRecordProcessor = new DpfRecordProcessor(serviceBroker);

    @Before
    public void setupMocks() throws BibliographicRecordFactoryException, UpdateServiceDoubleRecordCheckConnectorException {
        when(serviceBroker.isDoubleRecord(any())).thenReturn(false);
    }

    @Test
    public void initialProcessingInstructionsContainsErrors() throws DpfRecordProcessorException {
        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1").withErrors(Collections.singletonList("error"));
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, new MarcRecord());
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, new MarcRecord());

        assertThat(dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY))));
    }

    @Test
    public void newDpfRecordFailsDoubleRecordCheck()
            throws DpfRecordProcessorException, BibliographicRecordFactoryException,
                   UpdateServiceDoubleRecordCheckConnectorException {
        when(serviceBroker.isDoubleRecord(any())).thenReturn(true);

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.NEW);
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, new MarcRecord());
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, new MarcRecord());

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK),
                        new DpfRecordProcessor.Event("id-1", IS_DOUBLE_RECORD),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY))));
        assertThat("error in processing instructions", dpfRecord1.getErrors(),
                is(Collections.singletonList("dobbeltpost")));
        assertThat("error in body", dpfRecord1.getBody().hasField(
                hasTag("e99").and(hasSubFieldValue('b', "dobbeltpost"))),
                is(true));
    }

    @Test
    public void eventLogWithoutSuffix() {
        assertThat(new DpfRecordProcessor.Event("id", SENT_TO_LOBBY).toString(),
                is("id: Sent to lobby"));
    }

    @Test
    public void eventLogWithSuffix() {
        assertThat(new DpfRecordProcessor.Event("id", SENT_TO_LOBBY, "posthaste").toString(),
                is("id: Sent to lobby posthaste"));
    }
}