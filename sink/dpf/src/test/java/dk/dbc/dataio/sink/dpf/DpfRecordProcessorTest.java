/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.ProcessingInstructions;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnectorException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.IS_DOUBLE_RECORD;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.NEW_CATALOGUE_CODE;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.NEW_FAUST;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.PROCESS_AS_NEW;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.PROCESS_HEAD;
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

    @Test
    public void processAsNew_Single_DPFCodeIsDPF() throws Exception {
        DataField dataField = new DataField("032", "00");
        dataField.addSubField(new SubField('b', "DPF"));
        MarcRecord marcRecord = new MarcRecord();
        marcRecord.addAllFields(Arrays.asList(dataField));

        when(serviceBroker.isDoubleRecord(any())).thenReturn(false);
        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.NEW);
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecord);

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK),
                        new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945")
                )));

        assertThat("record", dpfRecord1.getCatalogueCode(), is("DPF201945"));
        assertThat("record", dpfRecord1.getDPFCode() == null); // Fix to proper null check
        assertThat("systemControlNumbers", getSystemControlNumbers(dpfRecord1), is(Arrays.asList(
                "(DK-870970)1234"
        )));
    }

    @Test
    public void processAsNew_Single_DPFCodeIsNotDPF() throws Exception {
        DataField dataField = new DataField("032", "00");
        dataField.addSubField(new SubField('b', "NOPE"));
        MarcRecord marcRecord = new MarcRecord();
        marcRecord.addAllFields(Arrays.asList(dataField));

        when(serviceBroker.isDoubleRecord(any())).thenReturn(false);
        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.NEW);
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecord);

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK),
                        new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945")
                )));

        assertThat("record", dpfRecord1.getCatalogueCode() == null);
        assertThat("record", dpfRecord1.getDPFCode(), is("NOPE"));
        assertThat("systemControlNumbers", getSystemControlNumbers(dpfRecord1), is(Arrays.asList(
                "(DK-870970)1234"
        )));
    }

    @Test
    public void processAsNew_Head_DPFCodeIsDPF() throws Exception {
        DataField dataField = new DataField("032", "00");
        dataField.addSubField(new SubField('b', "DPF"));
        MarcRecord marcRecordDpf = new MarcRecord();
        marcRecordDpf.addAllFields(Arrays.asList(dataField));

        MarcRecord marcRecordHead = new MarcRecord();

        when(serviceBroker.isDoubleRecord(any())).thenReturn(false);
        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.NEW);
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecordDpf);
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, marcRecordHead);

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK),
                        new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-2", NEW_FAUST, "1234")
                )));

        assertThat("dpf faust", dpfRecord1.getBibliographicRecordId(), is("1234"));
        assertThat("dpf catalogueCode", dpfRecord1.getCatalogueCode(), is("DPF201945"));
        assertThat("dpf dpf-code", dpfRecord1.getDPFCode() == null); // Fix to proper null check
        assertThat("systemControlNumbers", getSystemControlNumbers(dpfRecord1), is(Arrays.asList(
                "(DK-870970)1234",
                "(DPFHOVED)1234"
        )));

        assertThat("head faust", dpfRecord2.getBibliographicRecordId(), is("1234"));
        assertThat("head otherBibliographicRecordId", dpfRecord2.getOtherBibliographicRecordId(), is("1234"));
    }

    @Test
    public void processAsNew_Head_DPFCodeIsNotDPF() throws Exception {
        DataField dataField = new DataField("032", "00");
        dataField.addSubField(new SubField('b', "NOPE"));
        MarcRecord marcRecordDpf = new MarcRecord();
        marcRecordDpf.addAllFields(Arrays.asList(dataField));

        MarcRecord marcRecordHead = new MarcRecord();

        when(serviceBroker.isDoubleRecord(any())).thenReturn(false);
        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.NEW);
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecordDpf);
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, marcRecordHead);

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK),
                        new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-2", NEW_FAUST, "1234")
                )));

        assertThat("dpf faust", dpfRecord1.getBibliographicRecordId(), is("1234"));
        assertThat("dpf catalogueCode", dpfRecord1.getCatalogueCode() == null);
        assertThat("dpf dpf-code", dpfRecord1.getDPFCode(), is("NOPE"));
        assertThat("systemControlNumbers", getSystemControlNumbers(dpfRecord1), is(Arrays.asList(
                "(DK-870970)1234",
                "(DPFHOVED)1234"
        )));

        assertThat("head faust", dpfRecord2.getBibliographicRecordId(), is("1234"));
        assertThat("head otherBibliographicRecordId", dpfRecord2.getOtherBibliographicRecordId(), is("1234"));
    }

    private List<String> getSystemControlNumbers(DpfRecord dpfRecord) {
        List<String> systemControlNumbers = new ArrayList<>();

        for (Field field : dpfRecord.getBody().getFields()) {
            if ("035".equals(field.getTag())) {
                DataField dataField = (DataField) field;
                for (SubField subField : dataField.getSubFields()) {
                    if ('a' == subField.getCode()) {
                        systemControlNumbers.add(subField.getData());
                    }
                }
            }
        }

        return systemControlNumbers;
    }

}