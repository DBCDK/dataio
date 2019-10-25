/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.ProcessingInstructions;
import dk.dbc.marc.binding.MarcRecord;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class DpfRecordProcessorTest {
    private final ServiceBroker serviceBroker = mock(ServiceBroker.class);
    private final DpfRecordProcessor dpfRecordProcessor = new DpfRecordProcessor(serviceBroker);

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
                        new DpfRecordProcessor.Event("id-1", DpfRecordProcessor.Event.Type.SENT_TO_LOBBY),
                        new DpfRecordProcessor.Event("id-2", DpfRecordProcessor.Event.Type.SENT_TO_LOBBY))));
    }
}