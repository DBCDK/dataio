/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.ProcessingInstructions;
import dk.dbc.dataio.sink.dpf.model.RawrepoRecord;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.oss.ns.catalogingupdate.DoubleRecordEntries;
import dk.dbc.oss.ns.catalogingupdate.DoubleRecordEntry;
import dk.dbc.oss.ns.catalogingupdate.MessageEntry;
import dk.dbc.oss.ns.catalogingupdate.Messages;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnectorException;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.DIFFERENT_PERIODICA_TYPE;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.DPF_REFERENCE_MISMATCH;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.IS_DOUBLE_RECORD;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.NEW_CATALOGUE_CODE;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.NEW_FAUST;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.NOT_FOUND;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.PROCESS_AS_MODIFIED;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.PROCESS_AS_NEW;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.PROCESS_HEAD;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.SENT_TO_DOUBLE_RECORD_CHECK;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.SENT_TO_LOBBY;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.SENT_TO_UPDATESERVICE;
import static dk.dbc.dataio.sink.dpf.DpfRecordProcessor.Event.Type.UPDATE_VALIDATION_ERROR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DpfRecordProcessorTest {
    private final String queueProvider = "queueProvider";
    private final ServiceBroker serviceBroker = mock(ServiceBroker.class);
    private final DpfRecordProcessor dpfRecordProcessor = new DpfRecordProcessor(serviceBroker, queueProvider);

    @Before
    public void setupMocks() throws BibliographicRecordFactoryException, UpdateServiceDoubleRecordCheckConnectorException {
        when(serviceBroker.isDoubleRecord(any())).thenReturn(createOKUpdateRecordResult());
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
        when(serviceBroker.isDoubleRecord(any())).thenReturn(createDoubleRecordErrorResult(Arrays.asList(
                "Double record for record 5 158 076 1, reason: 021e, 021e",
                "Double record for record 5 158 076 2, reason: 021e, 021e"
        )));

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
                is(Arrays.asList("Double record for record 5 158 076 1, reason: 021e, 021e",
                        "Double record for record 5 158 076 2, reason: 021e, 021e")));
        assertThat("double record messages", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Arrays.asList(
                "Double record for record 5 158 076 1, reason: 021e, 021e",
                "Double record for record 5 158 076 2, reason: 021e, 021e"
        )));
        assertThat("errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER
        )));
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
        final MarcRecord marcRecord = new MarcRecord();
        marcRecord.addField(createDataField("032", Collections.singletonList(
                new SubField('a', "DPF")
        )));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.NEW)
                .withUpdateTemplate("dbcperiodica");
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecord);

        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider))
                .thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK),
                        new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE)
                )));

        assertThat("dpf catalogue code", dpfRecord1.getCatalogueCodes(), is(Arrays.asList("DPF201945")));
        assertThat("dpf systemControlNumbers", dpfRecord1.getBody().getSubFieldValues("035", 'a'), is(Collections.singletonList(
                "(DK-870970)1234"
        )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
    }

    @Test
    public void processAsNew_Single_UpdateValidationErrors() throws Exception {
        final MarcRecord marcRecord = new MarcRecord();
        marcRecord.addField(createDataField("032", Collections.singletonList(
                new SubField('a', "DPF")
        )));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.NEW)
                .withUpdateTemplate("dbcperiodica");
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecord);

        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider))
                .thenReturn(createErrorUpdateRecordResult(Arrays.asList(
                        "Error 1",
                        "Error 2"
                )));
        when(serviceBroker.getUpdateErrors(eq("e01"), any(UpdateRecordResult.class), eq(dpfRecord1)))
                .thenReturn(Arrays.asList(
                        new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 1")),
                        new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 2"))));

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK),
                        new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE),
                        new DpfRecordProcessor.Event("id-1", UPDATE_VALIDATION_ERROR),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY)
                )));

        assertThat("dpf catalogue code", dpfRecord1.getCatalogueCodes(), is(Arrays.asList("DPF201945")));
        assertThat("dpf systemControlNumbers", dpfRecord1.getBody().getSubFieldValues("035", 'a'), is(Collections.singletonList(
                "(DK-870970)1234"
        )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e01", 'a'), is(Arrays.asList(
                "Error 1",
                "Error 2"
        )));
    }

    @Test
    public void processAsNew_Head_DPFCodeIsDPF() throws Exception {
        final MarcRecord marcRecordDpf = new MarcRecord();
        marcRecordDpf.addField(createDataField("032", Arrays.asList(
                new SubField('a', "DPF"),
                new SubField('a', "GPG"),
                new SubField('a', "FPF")
        )));

        final MarcRecord marcRecordHead = new MarcRecord();

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.NEW)
                .withUpdateTemplate("dbcperiodica");
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecordDpf);
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2")
                .withUpdateTemplate("dbchoved");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, marcRecordHead);

        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode("DPF")).thenReturn("DPF201945");
        when(serviceBroker.getCatalogueCode("GPG")).thenReturn("GPG201945");
        when(serviceBroker.getCatalogueCode("FPF")).thenReturn("FPF201945");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider))
                .thenReturn(createOKUpdateRecordResult());
        when(serviceBroker.sendToUpdate("010100", "dbchoved", dpfRecord2, "id-2", queueProvider))
                .thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK),
                        new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "GPG201945"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "FPF201945"),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-2", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_UPDATESERVICE)
                )));

        assertThat("dpf faust", dpfRecord1.getBibliographicRecordId(), is("1234"));
        assertThat("dpf catalogueCode", dpfRecord1.getBody().getSubFieldValues("032", 'a'), is(Arrays.asList(
                "DPF201945",
                "GPG201945",
                "FPF201945"
        )));
        assertThat("dpf systemControlNumbers", dpfRecord1.getBody().getSubFieldValues("035", 'a'), is(Arrays.asList( // TODO check there are two 035 fields
                "(DK-870970)1234",
                "(DPFHOVED)1234"
        )));

        assertThat("head faust", dpfRecord2.getBibliographicRecordId(), is("1234"));
        assertThat("head otherBibliographicRecordId", dpfRecord2.getOtherBibliographicRecordId(), is("1234"));
    }

    @Test
    public void processAsNew_Head_UpdateValidationErrorsDPF() throws Exception {
        final MarcRecord marcRecordDpf = new MarcRecord();
        marcRecordDpf.addField(createDataField("032", Collections.singletonList(
                new SubField('a', "DPF")
        )));

        final MarcRecord marcRecordHead = new MarcRecord();

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.NEW)
                .withUpdateTemplate("dbcperiodica");
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecordDpf);
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2")
                .withUpdateTemplate("dbchoved");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, marcRecordHead);

        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider))
                .thenReturn(createErrorUpdateRecordResult(Arrays.asList(
                        "Error 1",
                        "Error 2"
                )));
        when(serviceBroker.getUpdateErrors(eq("e01"), any(UpdateRecordResult.class), eq(dpfRecord1)))
                .thenReturn(Arrays.asList(
                        new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 1")),
                        new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 2"))));

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK),
                        new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-2", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE),
                        new DpfRecordProcessor.Event("id-1", UPDATE_VALIDATION_ERROR),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY)
                )));

        assertThat("dpf faust", dpfRecord1.getBibliographicRecordId(), is("1234"));
        assertThat("dpf catalogueCode", dpfRecord1.getCatalogueCodes(), is(Arrays.asList("DPF201945")));
        assertThat("dpf systemControlNumbers", dpfRecord1.getBody().getSubFieldValues("035", 'a'), is(Arrays.asList(
                "(DK-870970)1234",
                "(DPFHOVED)1234"
        )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e01", 'a'), is(Arrays.asList(
                "Error 1",
                "Error 2"
        )));
        assertThat("head faust", dpfRecord2.getBibliographicRecordId(), is("1234"));
        assertThat("head otherBibliographicRecordId", dpfRecord2.getOtherBibliographicRecordId(), is("1234"));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER
        )));
    }

    @Test
    public void processAsNew_Head_UpdateValidationErrorsHead() throws Exception {
        final MarcRecord marcRecordDpf = new MarcRecord();
        marcRecordDpf.addField(createDataField("032", Collections.singletonList(
                new SubField('a', "DPF")
        )));

        final MarcRecord marcRecordHead = new MarcRecord();

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.NEW)
                .withUpdateTemplate("dbcperiodica");
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecordDpf);
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2")
                .withUpdateTemplate("dbchoved");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, marcRecordHead);

        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider))
                .thenReturn(createOKUpdateRecordResult());
        when(serviceBroker.sendToUpdate("010100", "dbchoved", dpfRecord2, "id-2", queueProvider))
                .thenReturn(createErrorUpdateRecordResult(Arrays.asList(
                        "Error 1",
                        "Error 2"
                )));
        when(serviceBroker.getUpdateErrors(eq("e01"), any(UpdateRecordResult.class), eq(dpfRecord2)))
                .thenReturn(Arrays.asList(
                        new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 1")),
                        new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 2"))));

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK),
                        new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-2", NEW_FAUST, "1234"),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_UPDATESERVICE),
                        new DpfRecordProcessor.Event("id-2", UPDATE_VALIDATION_ERROR),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY)
                )));

        assertThat("dpf faust", dpfRecord1.getBibliographicRecordId(), is("1234"));
        assertThat("dpf catalogueCode", dpfRecord1.getCatalogueCodes(), is(Arrays.asList("DPF201945")));
        assertThat("dpf systemControlNumbers", dpfRecord1.getBody().getSubFieldValues("035", 'a'), is(Arrays.asList(
                "(DK-870970)1234",
                "(DPFHOVED)1234"
        )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER
        )));

        assertThat("head faust", dpfRecord2.getBibliographicRecordId(), is("1234"));
        assertThat("head otherBibliographicRecordId", dpfRecord2.getOtherBibliographicRecordId(), is("1234"));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e01", 'a'), is(Arrays.asList(
                "Error 1",
                "Error 2"
        )));
    }

    @Test
    public void processAsModified_single_NotFound() throws Exception {
        final MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.MODIFIED);
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(false);

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-1", NOT_FOUND, "1234:870970"),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY)
                )));

        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                String.format(DpfRecordProcessor.RECORD_NOT_FOUND, "1234", "870970")
        )));
    }

    @Test
    public void processAsModified_single_DifferentPeriodicaType() throws Exception {
        final MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Arrays.asList(new SubField('a', "1234"), new SubField('b', "870970"))));
        dpfBody.addField(createDataField("008", Arrays.asList(new SubField('h', "a"), new SubField('b', "870970"))));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.MODIFIED);
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);

        final MarcRecord existingBody = new MarcRecord();
        existingBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        existingBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "b"))));

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(existingBody));

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-1", DIFFERENT_PERIODICA_TYPE),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY)
                )));

        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                String.format(DpfRecordProcessor.CHANGED_PERIODICA, "b", "a")
        )));
    }

    @Test
    public void processAsModified_single_Ok() throws Exception {
        final MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "a"))));
        dpfBody.addField(createDataField("032", Arrays.asList(
                new SubField('a', "DPF"),
                new SubField('a', "GPG")
        )));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.MODIFIED)
                .withUpdateTemplate("dbcperiodica");
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);

        final MarcRecord existingBody = new MarcRecord();
        existingBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        existingBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "a"))));
        existingBody.addField(createDataField("032", Arrays.asList(
                new SubField('a', "DPF201945"),
                new SubField('a', "GPG201945")
        )));

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(existingBody));
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider))
                .thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE)
                )));
        assertThat("dpf catalogueCode", dpfRecord1.getBody().getSubFieldValues("032", 'a'), is(Arrays.asList(
                "DPF201945",
                "GPG201945"
        )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
    }

    @Test
    public void processAsClosed_single_Ok() throws Exception {
        final MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "a"))));
        dpfBody.addField(createDataField("032", Collections.singletonList(
                new SubField('a', "PFP")
        )));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.CLOSED)
                .withUpdateTemplate("dbcperiodica");
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);

        final MarcRecord existingBody = new MarcRecord();
        existingBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        existingBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "a"))));
        existingBody.addField(createDataField("032", Arrays.asList(
                new SubField('a', "DPF201945"),
                new SubField('a', "GPG201945")
        )));

        when(serviceBroker.getCatalogueCode(any())).thenReturn("PFP201946");
        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(existingBody));
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider))
                .thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "PFP201946"),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE)
                )));
        assertThat("dpf catalogueCode", dpfRecord1.getBody().getSubFieldValues("032", 'a'), is(Arrays.asList(
                "PFP201946",
                "DPF201945",
                "GPG201945"
        )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
    }

    @Test
    public void processAsModified_head_NotFound() throws Exception {
        final MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        dpfBody.addField(createDataField("035", Arrays.asList(
                new SubField('a', "((DK-870970)1234"),
                new SubField('a', "(DPFHOVED)5678")
        )));

        final MarcRecord headBody = new MarcRecord();
        headBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.MODIFIED);
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, headBody);

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.rawrepoRecordExists("5678", 870970)).thenReturn(false);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(dpfBody));

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-2", NOT_FOUND, "5678:870970"),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY)
                )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER
        )));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                String.format(DpfRecordProcessor.RECORD_NOT_FOUND, "5678", "870970")
        )));
    }

    @Test
    public void processAsModified_head_ReferenceMismatchNull() throws Exception {
        final MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        dpfBody.addField(createDataField("035", Arrays.asList(
                new SubField('a', "((DK-870970)1234"),
                new SubField('a', "(DPFHOVED)5678")
        )));

        final MarcRecord headBody = new MarcRecord();
        headBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.MODIFIED);
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, headBody);

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.rawrepoRecordExists("5678", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(dpfBody));
        when(serviceBroker.getRawrepoRecord("5678", 870970)).thenReturn(new RawrepoRecord(headBody));

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-2", DPF_REFERENCE_MISMATCH),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY)
                )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER
        )));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                String.format(DpfRecordProcessor.REFERENCE_MISMATCH, "1234", "870970", "null", "870970")
        )));
    }

    @Test
    public void processAsModified_head_ReferenceMismatch() throws Exception {
        final MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        dpfBody.addField(createDataField("035", Arrays.asList(
                new SubField('a', "((DK-870970)1234"),
                new SubField('a', "(DPFHOVED)5678")
        )));

        final MarcRecord headBody = new MarcRecord();
        headBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));

        final MarcRecord existingHeadBody = new MarcRecord();
        existingHeadBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));
        existingHeadBody.addField(createDataField("018", Collections.singletonList(new SubField('a', "4321"))));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.MODIFIED);
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, headBody);

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.rawrepoRecordExists("5678", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(dpfBody));
        when(serviceBroker.getRawrepoRecord("5678", 870970)).thenReturn(new RawrepoRecord(existingHeadBody));

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-2", DPF_REFERENCE_MISMATCH),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY)
                )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER
        )));

        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(
                String.format(DpfRecordProcessor.REFERENCE_MISMATCH, "1234", "870970", "4321", "870970")
        )));
    }

    @Test
    public void processAsModified_head_Ok() throws Exception {
        final MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        dpfBody.addField(createDataField("035", Arrays.asList(
                new SubField('a', "((DK-870970)1234"),
                new SubField('a', "(DPFHOVED)5678")
        )));

        final MarcRecord headBody = new MarcRecord();
        headBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));

        final MarcRecord existingHeadBody = new MarcRecord();
        existingHeadBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));
        existingHeadBody.addField(createDataField("018", Collections.singletonList(new SubField('a', "1234"))));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.MODIFIED)
                .withUpdateTemplate("dbcperiodica");
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2")
                .withUpdateTemplate("dbchoved");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, headBody);

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.rawrepoRecordExists("5678", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(dpfBody));
        when(serviceBroker.getRawrepoRecord("5678", 870970)).thenReturn(new RawrepoRecord(existingHeadBody));
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider))
                .thenReturn(createOKUpdateRecordResult());
        when(serviceBroker.sendToUpdate("010100", "dbchoved", dpfRecord2, "id-2", queueProvider))
                .thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_UPDATESERVICE)
                )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
    }

    @Test
    public void processAsClosed_head_Ok() throws Exception {
        final MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        dpfBody.addField(createDataField("032", Collections.singletonList(
                new SubField('a', "PFP")
        )));
        dpfBody.addField(createDataField("035", Arrays.asList(
                new SubField('a', "((DK-870970)1234"),
                new SubField('a', "(DPFHOVED)5678")
        )));

        final MarcRecord headBody = new MarcRecord();
        headBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));

        final MarcRecord existingBody = new MarcRecord();
        existingBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        existingBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        existingBody.addField(createDataField("032", Arrays.asList(
                new SubField('a', "DPF201945"),
                new SubField('a', "GPG201945")
        )));
        existingBody.addField(createDataField("035", Arrays.asList(
                new SubField('a', "((DK-870970)1234"),
                new SubField('a', "(DPFHOVED)5678")
        )));

        final MarcRecord existingHeadBody = new MarcRecord();
        existingHeadBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));
        existingHeadBody.addField(createDataField("018", Collections.singletonList(new SubField('a', "1234"))));

        final ProcessingInstructions processingInstructions1 = new ProcessingInstructions()
                .withId("id-1")
                .withRecordState(DpfRecord.State.CLOSED)
                .withUpdateTemplate("dbcperiodica");
        final DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);
        final ProcessingInstructions processingInstructions2 = new ProcessingInstructions()
                .withId("id-2")
                .withUpdateTemplate("dbchoved");
        final DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, headBody);

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.rawrepoRecordExists("5678", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(existingBody));
        when(serviceBroker.getRawrepoRecord("5678", 870970)).thenReturn(new RawrepoRecord(existingHeadBody));
        when(serviceBroker.getCatalogueCode(any())).thenReturn("PFP201946");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider))
                .thenReturn(createOKUpdateRecordResult());
        when(serviceBroker.sendToUpdate("010100", "dbchoved", dpfRecord2, "id-2", queueProvider))
                .thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(
                        new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "PFP201946"),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_UPDATESERVICE)
                )));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
        assertThat("dpf catalogueCode", dpfRecord1.getBody().getSubFieldValues("032", 'a'), is(Arrays.asList(
                "PFP201946",
                "DPF201945",
                "GPG201945"
        )));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
    }

    private DataField createDataField(String code, List<SubField> subFields) {
        final DataField dataField = new DataField(code, "00");
        dataField.addAllSubFields(subFields);

        return dataField;
    }

    private UpdateRecordResult createOKUpdateRecordResult() {
        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.OK);

        return updateRecordResult;
    }

    private UpdateRecordResult createDoubleRecordErrorResult(List<String> messageStrings) {
        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);

        final DoubleRecordEntries doubleRecordEntries = new DoubleRecordEntries();
        for (String message : messageStrings) {
            final DoubleRecordEntry entry = new DoubleRecordEntry();
            entry.setMessage(message);
            doubleRecordEntries.getDoubleRecordEntry().add(entry);
        }
        updateRecordResult.setDoubleRecordEntries(doubleRecordEntries);
        return updateRecordResult;
    }

    private UpdateRecordResult createErrorUpdateRecordResult(List<String> messageStrings) {
        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);

        final Messages messages = new Messages();
        for (String message : messageStrings) {
            final MessageEntry messageEntry = new MessageEntry();
            messageEntry.setMessage(message);

            messages.getMessageEntry().add(messageEntry);
        }
        updateRecordResult.setMessages(messages);

        return updateRecordResult;
    }

}