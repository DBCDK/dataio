package dk.dbc.dataio.sink.dpf.transform;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.sink.dpf.ServiceBroker;
import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.ProcessingInstructions;
import dk.dbc.dataio.sink.dpf.model.RawrepoRecord;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.oss.ns.catalogingupdate.MessageEntry;
import dk.dbc.oss.ns.catalogingupdate.Messages;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnectorException;
import dk.dbc.updateservice.dto.DoubleRecordFrontendDTO;
import dk.dbc.updateservice.dto.UpdateRecordResponseDTO;
import dk.dbc.updateservice.dto.UpdateStatusEnumDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.DBC_PROTECTED_FIELDS;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.DIFFERENT_PERIODICA_TYPE;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.DPF_REFERENCE_MISMATCH;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.IS_DOUBLE_RECORD;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.NEW_CATALOGUE_CODE;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.NEW_FAUST;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.NOT_FOUND;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.PROCESS_AS_MODIFIED;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.PROCESS_AS_NEW;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.PROCESS_HEAD;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.SENT_TO_DOUBLE_RECORD_CHECK;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.SENT_TO_LOBBY;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.SENT_TO_UPDATESERVICE;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.Event.Type.UPDATE_VALIDATION_ERROR;
import static dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor.PROTECTED_FIELDS;
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

    @BeforeEach
    public void setupMocks() throws UpdateServiceDoubleRecordCheckConnectorException, JSONBException {
        when(serviceBroker.isDoubleRecord(any())).thenReturn(createOKDoubleRecordResult());
    }

    @Test
    public void initialProcessingInstructionsContainsErrors() throws DpfRecordProcessorException {
        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withErrors(Collections.singletonList("error"));
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, new MarcRecord());
        ProcessingInstructions processingInstructions2 = new ProcessingInstructions().withId("id-2");
        DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, new MarcRecord());

        assertThat(dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY), new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY))));
    }

    private void createExistingRecord(MarcRecord record) {
        record.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        record.addField(createDataField("008", Collections.singletonList(new SubField('h', "a"))));
        record.addField(createDataField("032", Arrays.asList(new SubField('a', "PFP201946"),
                new SubField('a', "DPF201945"),
                new SubField('a', "GPG201945"))));
        for (String field : PROTECTED_FIELDS) {
            record.addField(createDataField(field, Arrays.asList(new SubField('&', "810012"), new SubField('a', "Tekst"))));
        }
        for (String field : DBC_PROTECTED_FIELDS) {
            record.addField(createDataField(field, List.of(new SubField('a', "Tekst"))));
        }
        record.addField(createDataField("504", Collections.singletonList(new SubField('a', "Overskrivelig note"))));
        record.addField(createDataField("666", Collections.singletonList(new SubField('e', "Overskriveligt emneord"))));
    }

    @Test
    public void newDpfRecordCopyExistingFields() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "a"))));
        dpfBody.addField(createDataField("032", Collections.singletonList(new SubField('a', "FPF201946"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1")
                .withRecordState(DpfRecord.State.CLOSED).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);

        MarcRecord existingBody = new MarcRecord();
        createExistingRecord(existingBody);

        when(serviceBroker.getCatalogueCode(any())).thenReturn("PFP201946");
        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(existingBody));
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider))
                .thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)),
                is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE))));

        assertThat("dpf catalogueCode", dpfRecord1.getBody().getSubFieldValues("032", 'a'), is(Arrays.asList("FPF201946", "PFP201946", "DPF201945", "GPG201945")));
        for (String field : PROTECTED_FIELDS) {
            assertThat("dpf catalogueCode " + field, dpfRecord1.getBody().getSubFieldValues(field, '&'), is(List.of("810012")));
            assertThat("dpf catalogueCode " + field, dpfRecord1.getBody().getSubFieldValues(field, 'a'), is(List.of("Tekst")));
        }
        for (String field : DBC_PROTECTED_FIELDS) {
            assertThat("dpf catalogueCode " + field, dpfRecord1.getBody().getSubFieldValues(field, 'a'), is(List.of("Tekst")));
        }
        // Just for verify
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));

    }

    @Test
    public void newDpfRecordCopyExistingFieldsToLobbyDifferentPeriodicaType() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "b"))));
        dpfBody.addField(createDataField("032", Collections.singletonList(new SubField('a', "FPF201946"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.CLOSED).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);

        MarcRecord existingBody = new MarcRecord();
        createExistingRecord(existingBody);

        when(serviceBroker.getCatalogueCode(any())).thenReturn("PFP201946");
        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(existingBody));
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider)).thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED), new DpfRecordProcessor.Event("id-1", DIFFERENT_PERIODICA_TYPE), new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY))));

        assertThat("dpf catalogueCode", dpfRecord1.getBody().getSubFieldValues("032", 'a'), is(Arrays.asList("FPF201946", "PFP201946", "DPF201945", "GPG201945")));

        for (String field : PROTECTED_FIELDS) {
            assertThat("dpf catalogueCode " + field, dpfRecord1.getBody().getSubFieldValues(field, '&'), is(List.of("810012")));
            assertThat("dpf catalogueCode " + field, dpfRecord1.getBody().getSubFieldValues(field, 'a'), is(List.of("Tekst")));
        }
        for (String field : DBC_PROTECTED_FIELDS) {
            assertThat("dpf catalogueCode " + field, dpfRecord1.getBody().getSubFieldValues(field, 'a'), is(List.of("Tekst")));
        }
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(String.format(DpfRecordProcessor.CHANGED_PERIODICA, "a", "b"))));

    }

    @Test
    public void newDpfRecordFailsDoubleRecordCheck() throws DpfRecordProcessorException, UpdateServiceDoubleRecordCheckConnectorException, JSONBException {
        when(serviceBroker.isDoubleRecord(any())).thenReturn(createDoubleRecordErrorResult(Arrays.asList("Double record for record 5 158 076 1, reason: 021e, 021e", "Double record for record 5 158 076 2, reason: 021e, 021e")));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.NEW);
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, new MarcRecord());
        ProcessingInstructions processingInstructions2 = new ProcessingInstructions().withId("id-2");
        DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, new MarcRecord());

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW), new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK), new DpfRecordProcessor.Event("id-1", IS_DOUBLE_RECORD), new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY), new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY))));
        assertThat("error in processing instructions", dpfRecord1.getErrors(), is(Arrays.asList("Double record for record 5 158 076 1, reason: 021e, 021e", "Double record for record 5 158 076 2, reason: 021e, 021e")));
        assertThat("double record messages", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Arrays.asList("Double record for record 5 158 076 1, reason: 021e, 021e", "Double record for record 5 158 076 2, reason: 021e, 021e")));
        assertThat("errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER)));
    }

    @Test
    public void eventLogWithoutSuffix() {
        assertThat(new DpfRecordProcessor.Event("id", SENT_TO_LOBBY).toString(), is("id: Sent to lobby"));
    }

    @Test
    public void eventLogWithSuffix() {
        assertThat(new DpfRecordProcessor.Event("id", SENT_TO_LOBBY, "posthaste").toString(), is("id: Sent to lobby posthaste"));
    }

    @Test
    public void processAsNew_Single_DPFCodeIsDPF() throws Exception {
        MarcRecord marcRecord = new MarcRecord();
        marcRecord.addField(createDataField("032", Collections.singletonList(new SubField('a', "DPF"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.NEW).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecord);

        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider)).thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW), new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK), new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"), new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"), new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE))));

        assertThat("dpf catalogue code", dpfRecord1.getCatalogueCodes(), is(List.of("DPF201945")));
        assertThat("dpf systemControlNumbers", dpfRecord1.getBody().getSubFieldValues("035", 'a'), is(Collections.singletonList("(DK-870970)1234")));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
    }

    @Test
    public void processAsNew_Single_UpdateValidationErrors() throws Exception {
        MarcRecord marcRecord = new MarcRecord();
        marcRecord.addField(createDataField("032", Collections.singletonList(new SubField('a', "DPF"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.NEW).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecord);

        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider)).thenReturn(createErrorUpdateRecordResult(Arrays.asList("Error 1", "Error 2")));
        when(serviceBroker.getUpdateErrors(eq("e01"), any(UpdateRecordResult.class), eq(dpfRecord1))).thenReturn(Arrays.asList(new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 1")), new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 2"))));

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW), new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK), new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"), new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"), new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE), new DpfRecordProcessor.Event("id-1", UPDATE_VALIDATION_ERROR), new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY))));

        assertThat("dpf catalogue code", dpfRecord1.getCatalogueCodes(), is(List.of("DPF201945")));
        assertThat("dpf systemControlNumbers", dpfRecord1.getBody().getSubFieldValues("035", 'a'), is(Collections.singletonList("(DK-870970)1234")));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e01", 'a'), is(Arrays.asList("Error 1", "Error 2")));
    }

    @Test
    public void processAsNew_Head_DPFCodeIsDPF() throws Exception {
        MarcRecord marcRecordDpf = new MarcRecord();
        marcRecordDpf.addField(createDataField("032", Arrays.asList(new SubField('a', "DPF"), new SubField('a', "GPG"), new SubField('a', "FPF"))));

        MarcRecord marcRecordHead = new MarcRecord();

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.NEW).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecordDpf);
        ProcessingInstructions processingInstructions2 = new ProcessingInstructions().withId("id-2").withUpdateTemplate("dbchoved");
        DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, marcRecordHead);

        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode("DPF")).thenReturn("DPF201945");
        when(serviceBroker.getCatalogueCode("GPG")).thenReturn("GPG201945");
        when(serviceBroker.getCatalogueCode("FPF")).thenReturn("FPF201945");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider)).thenReturn(createOKUpdateRecordResult());
        when(serviceBroker.sendToUpdate("010100", "dbchoved", dpfRecord2, "id-2", queueProvider)).thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW), new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK), new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"), new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"), new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "GPG201945"), new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "FPF201945"), new DpfRecordProcessor.Event("id-2", PROCESS_HEAD), new DpfRecordProcessor.Event("id-2", NEW_FAUST, "1234"), new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE), new DpfRecordProcessor.Event("id-2", SENT_TO_UPDATESERVICE))));

        assertThat("dpf faust", dpfRecord1.getBibliographicRecordId(), is("1234"));
        assertThat("dpf catalogueCode", dpfRecord1.getBody().getSubFieldValues("032", 'a'), is(Arrays.asList("DPF201945", "GPG201945", "FPF201945")));
        assertThat("dpf systemControlNumbers", dpfRecord1.getBody().getSubFieldValues("035", 'a'), is(Arrays.asList( // TODO check there are two 035 fields
                "(DK-870970)1234", "(DPFHOVED)1234")));

        assertThat("head faust", dpfRecord2.getBibliographicRecordId(), is("1234"));
        assertThat("head otherBibliographicRecordId", dpfRecord2.getOtherBibliographicRecordId(), is("1234"));
    }

    @Test
    public void processAsNew_Head_UpdateValidationErrorsDPF() throws Exception {
        MarcRecord marcRecordDpf = new MarcRecord();
        marcRecordDpf.addField(createDataField("032", Collections.singletonList(new SubField('a', "DPF"))));

        MarcRecord marcRecordHead = new MarcRecord();

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.NEW).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecordDpf);
        ProcessingInstructions processingInstructions2 = new ProcessingInstructions().withId("id-2").withUpdateTemplate("dbchoved");
        DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, marcRecordHead);

        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider)).thenReturn(createErrorUpdateRecordResult(Arrays.asList("Error 1", "Error 2")));
        when(serviceBroker.getUpdateErrors(eq("e01"), any(UpdateRecordResult.class), eq(dpfRecord1))).thenReturn(Arrays.asList(new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 1")), new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 2"))));

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW), new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK), new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"), new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"), new DpfRecordProcessor.Event("id-2", PROCESS_HEAD), new DpfRecordProcessor.Event("id-2", NEW_FAUST, "1234"), new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE), new DpfRecordProcessor.Event("id-1", UPDATE_VALIDATION_ERROR), new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY), new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY))));

        assertThat("dpf faust", dpfRecord1.getBibliographicRecordId(), is("1234"));
        assertThat("dpf catalogueCode", dpfRecord1.getCatalogueCodes(), is(List.of("DPF201945")));
        assertThat("dpf systemControlNumbers", dpfRecord1.getBody().getSubFieldValues("035", 'a'), is(Arrays.asList("(DK-870970)1234", "(DPFHOVED)1234")));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e01", 'a'), is(Arrays.asList("Error 1", "Error 2")));
        assertThat("head faust", dpfRecord2.getBibliographicRecordId(), is("1234"));
        assertThat("head otherBibliographicRecordId", dpfRecord2.getOtherBibliographicRecordId(), is("1234"));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER)));
    }

    @Test
    public void processAsNew_Head_UpdateValidationErrorsHead() throws Exception {
        MarcRecord marcRecordDpf = new MarcRecord();
        marcRecordDpf.addField(createDataField("032", Collections.singletonList(new SubField('a', "DPF"))));

        MarcRecord marcRecordHead = new MarcRecord();

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.NEW).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, marcRecordDpf);
        ProcessingInstructions processingInstructions2 = new ProcessingInstructions().withId("id-2").withUpdateTemplate("dbchoved");
        DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, marcRecordHead);

        when(serviceBroker.getNewFaust()).thenReturn("1234");
        when(serviceBroker.getCatalogueCode(any())).thenReturn("DPF201945");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider)).thenReturn(createOKUpdateRecordResult());
        when(serviceBroker.sendToUpdate("010100", "dbchoved", dpfRecord2, "id-2", queueProvider)).thenReturn(createErrorUpdateRecordResult(Arrays.asList("Error 1", "Error 2")));
        when(serviceBroker.getUpdateErrors(eq("e01"), any(UpdateRecordResult.class), eq(dpfRecord2))).thenReturn(Arrays.asList(new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 1")), new DataField().setTag("e01").addSubField(new SubField().setCode('a').setData("Error 2"))));

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_NEW), new DpfRecordProcessor.Event("id-1", SENT_TO_DOUBLE_RECORD_CHECK), new DpfRecordProcessor.Event("id-1", NEW_FAUST, "1234"), new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "DPF201945"), new DpfRecordProcessor.Event("id-2", PROCESS_HEAD), new DpfRecordProcessor.Event("id-2", NEW_FAUST, "1234"), new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE), new DpfRecordProcessor.Event("id-2", SENT_TO_UPDATESERVICE), new DpfRecordProcessor.Event("id-2", UPDATE_VALIDATION_ERROR), new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY), new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY))));

        assertThat("dpf faust", dpfRecord1.getBibliographicRecordId(), is("1234"));
        assertThat("dpf catalogueCode", dpfRecord1.getCatalogueCodes(), is(List.of("DPF201945")));
        assertThat("dpf systemControlNumbers", dpfRecord1.getBody().getSubFieldValues("035", 'a'), is(Arrays.asList("(DK-870970)1234", "(DPFHOVED)1234")));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER)));

        assertThat("head faust", dpfRecord2.getBibliographicRecordId(), is("1234"));
        assertThat("head otherBibliographicRecordId", dpfRecord2.getOtherBibliographicRecordId(), is("1234"));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e01", 'a'), is(Arrays.asList("Error 1", "Error 2")));
    }

    @Test
    public void processAsModified_single_NotFound() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.MODIFIED);
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(false);

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED), new DpfRecordProcessor.Event("id-1", NOT_FOUND, "1234:870970"), new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY))));

        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(String.format(DpfRecordProcessor.RECORD_NOT_FOUND, "1234", "870970"))));
    }

    @Test
    public void processAsModified_single_DifferentPeriodicaType() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Arrays.asList(new SubField('a', "1234"), new SubField('b', "870970"))));
        dpfBody.addField(createDataField("008", Arrays.asList(new SubField('h', "a"), new SubField('b', "870970"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.MODIFIED);
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);

        MarcRecord existingBody = new MarcRecord();
        existingBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        existingBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "b"))));

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(existingBody));

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED), new DpfRecordProcessor.Event("id-1", DIFFERENT_PERIODICA_TYPE), new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY))));

        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(String.format(DpfRecordProcessor.CHANGED_PERIODICA, "b", "a"))));
    }

    @Test
    public void processAsModified_single_Ok() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "a"))));
        dpfBody.addField(createDataField("032", Arrays.asList(new SubField('a', "DPF"), new SubField('a', "GPG"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.MODIFIED).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);

        MarcRecord existingBody = new MarcRecord();
        existingBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        existingBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "a"))));
        existingBody.addField(createDataField("032", Arrays.asList(new SubField('a', "DPF201945"), new SubField('a', "GPG201945"))));

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(existingBody));
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider)).thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED), new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE))));
        assertThat("dpf catalogueCode", dpfRecord1.getBody().getSubFieldValues("032", 'a'), is(Arrays.asList("DPF201945", "GPG201945")));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
    }

    @Test
    public void processAsClosed_single_Ok() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "a"))));
        dpfBody.addField(createDataField("032", Collections.singletonList(new SubField('a', "PFP"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.CLOSED).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);

        MarcRecord existingBody = new MarcRecord();
        existingBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        existingBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "a"))));
        existingBody.addField(createDataField("032", Arrays.asList(new SubField('a', "DPF201945"), new SubField('a', "GPG201945"))));

        when(serviceBroker.getCatalogueCode(any())).thenReturn("PFP201946");
        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(existingBody));
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider)).thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Collections.singletonList(dpfRecord1)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED), new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "PFP201946"), new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE))));
        assertThat("dpf catalogueCode", dpfRecord1.getBody().getSubFieldValues("032", 'a'), is(Arrays.asList("PFP201946", "DPF201945", "GPG201945")));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
    }

    @Test
    public void processAsModified_head_NotFound() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        dpfBody.addField(createDataField("035", Arrays.asList(new SubField('a', "((DK-870970)1234"), new SubField('a', "(DPFHOVED)5678"))));

        MarcRecord headBody = new MarcRecord();
        headBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.MODIFIED);
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);
        dpfRecord1.setCatalogueCodeField(new DataField("032", "00"));
        ProcessingInstructions processingInstructions2 = new ProcessingInstructions().withId("id-2");
        DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, headBody);
        dpfRecord2.setCatalogueCodeField(new DataField("032", "00"));

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.rawrepoRecordExists("5678", 870970)).thenReturn(false);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(dpfBody));

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED), new DpfRecordProcessor.Event("id-2", PROCESS_HEAD), new DpfRecordProcessor.Event("id-2", NOT_FOUND, "5678:870970"), new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY), new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY))));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER)));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(String.format(DpfRecordProcessor.RECORD_NOT_FOUND, "5678", "870970"))));
    }

    @Test
    public void processAsModified_head_ReferenceMismatchNull() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        dpfBody.addField(createDataField("035", Arrays.asList(new SubField('a', "((DK-870970)1234"), new SubField('a', "(DPFHOVED)5678"))));

        MarcRecord headBody = new MarcRecord();
        headBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.MODIFIED);
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);
        dpfRecord1.setCatalogueCodeField(new DataField("032", "00"));
        ProcessingInstructions processingInstructions2 = new ProcessingInstructions().withId("id-2");
        DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, headBody);
        dpfRecord2.setCatalogueCodeField(new DataField("032", "00"));

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.rawrepoRecordExists("5678", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(dpfBody));
        when(serviceBroker.getRawrepoRecord("5678", 870970)).thenReturn(new RawrepoRecord(headBody));

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED), new DpfRecordProcessor.Event("id-2", PROCESS_HEAD), new DpfRecordProcessor.Event("id-2", DPF_REFERENCE_MISMATCH), new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY), new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY))));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER)));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(String.format(DpfRecordProcessor.REFERENCE_MISMATCH, "1234", "870970", "null", "870970"))));
    }

    @Test
    public void processAsModified_head_ReferenceMismatch() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        dpfBody.addField(createDataField("035", Arrays.asList(new SubField('a', "((DK-870970)1234"), new SubField('a', "(DPFHOVED)5678"))));

        MarcRecord headBody = new MarcRecord();
        headBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));

        MarcRecord existingHeadBody = new MarcRecord();
        existingHeadBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));
        existingHeadBody.addField(createDataField("018", Collections.singletonList(new SubField('a', "4321"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.MODIFIED);
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);
        dpfRecord1.setCatalogueCodeField(new DataField("032", "00"));
        ProcessingInstructions processingInstructions2 = new ProcessingInstructions().withId("id-2");
        DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, headBody);
        dpfRecord2.setCatalogueCodeField(new DataField("032", "00"));

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.rawrepoRecordExists("5678", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(dpfBody));
        when(serviceBroker.getRawrepoRecord("5678", 870970)).thenReturn(new RawrepoRecord(existingHeadBody));

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)),
                is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED),
                        new DpfRecordProcessor.Event("id-2", PROCESS_HEAD),
                        new DpfRecordProcessor.Event("id-2", DPF_REFERENCE_MISMATCH),
                        new DpfRecordProcessor.Event("id-1", SENT_TO_LOBBY),
                        new DpfRecordProcessor.Event("id-2", SENT_TO_LOBBY))));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(DpfRecordProcessor.FAILED_BECAUSE_OF_OTHER)));

        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.singletonList(String.format(DpfRecordProcessor.REFERENCE_MISMATCH, "1234", "870970", "4321", "870970"))));
    }

    @Test
    public void processAsModified_head_Ok() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        dpfBody.addField(createDataField("035", Arrays.asList(new SubField('a', "((DK-870970)1234"), new SubField('a', "(DPFHOVED)5678"))));

        MarcRecord headBody = new MarcRecord();
        headBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));

        MarcRecord existingHeadBody = new MarcRecord();
        existingHeadBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));
        existingHeadBody.addField(createDataField("018", Collections.singletonList(new SubField('a', "1234"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.MODIFIED).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);
        dpfRecord1.setCatalogueCodeField(new DataField("032", "00"));
        ProcessingInstructions processingInstructions2 = new ProcessingInstructions().withId("id-2").withUpdateTemplate("dbchoved");
        DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, headBody);
        dpfRecord2.setCatalogueCodeField(new DataField("032", "00"));

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.rawrepoRecordExists("5678", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(dpfBody));
        when(serviceBroker.getRawrepoRecord("5678", 870970)).thenReturn(new RawrepoRecord(existingHeadBody));
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider)).thenReturn(createOKUpdateRecordResult());
        when(serviceBroker.sendToUpdate("010100", "dbchoved", dpfRecord2, "id-2", queueProvider)).thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED), new DpfRecordProcessor.Event("id-2", PROCESS_HEAD), new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE), new DpfRecordProcessor.Event("id-2", SENT_TO_UPDATESERVICE))));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
    }

    @Test
    public void processAsClosed_head_Ok() throws Exception {
        MarcRecord dpfBody = new MarcRecord();
        dpfBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        dpfBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        dpfBody.addField(createDataField("032", Collections.singletonList(new SubField('a', "PFP"))));
        dpfBody.addField(createDataField("035", Arrays.asList(new SubField('a', "((DK-870970)1234"), new SubField('a', "(DPFHOVED)5678"))));

        MarcRecord headBody = new MarcRecord();
        headBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));

        MarcRecord existingBody = new MarcRecord();
        existingBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "1234"))));
        existingBody.addField(createDataField("008", Collections.singletonList(new SubField('h', "z"))));
        existingBody.addField(createDataField("032", Arrays.asList(new SubField('a', "DPF201945"), new SubField('a', "GPG201945"))));
        existingBody.addField(createDataField("035", Arrays.asList(new SubField('a', "((DK-870970)1234"), new SubField('a', "(DPFHOVED)5678"))));

        MarcRecord existingHeadBody = new MarcRecord();
        existingHeadBody.addField(createDataField("001", Collections.singletonList(new SubField('a', "5678"))));
        existingHeadBody.addField(createDataField("018", Collections.singletonList(new SubField('a', "1234"))));

        ProcessingInstructions processingInstructions1 = new ProcessingInstructions().withId("id-1").withRecordState(DpfRecord.State.CLOSED).withUpdateTemplate("dbcperiodica");
        DpfRecord dpfRecord1 = new DpfRecord(processingInstructions1, dpfBody);
        ProcessingInstructions processingInstructions2 = new ProcessingInstructions().withId("id-2").withUpdateTemplate("dbchoved");
        DpfRecord dpfRecord2 = new DpfRecord(processingInstructions2, headBody);

        when(serviceBroker.rawrepoRecordExists("1234", 870970)).thenReturn(true);
        when(serviceBroker.rawrepoRecordExists("5678", 870970)).thenReturn(true);
        when(serviceBroker.getRawrepoRecord("1234", 870970)).thenReturn(new RawrepoRecord(existingBody));
        when(serviceBroker.getRawrepoRecord("5678", 870970)).thenReturn(new RawrepoRecord(existingHeadBody));
        when(serviceBroker.getCatalogueCode(any())).thenReturn("PFP201946");
        when(serviceBroker.sendToUpdate("010100", "dbcperiodica", dpfRecord1, "id-1", queueProvider)).thenReturn(createOKUpdateRecordResult());
        when(serviceBroker.sendToUpdate("010100", "dbchoved", dpfRecord2, "id-2", queueProvider)).thenReturn(createOKUpdateRecordResult());

        assertThat("events", dpfRecordProcessor.process(Arrays.asList(dpfRecord1, dpfRecord2)), is(Arrays.asList(new DpfRecordProcessor.Event("id-1", PROCESS_AS_MODIFIED), new DpfRecordProcessor.Event("id-1", NEW_CATALOGUE_CODE, "PFP201946"), new DpfRecordProcessor.Event("id-2", PROCESS_HEAD), new DpfRecordProcessor.Event("id-1", SENT_TO_UPDATESERVICE), new DpfRecordProcessor.Event("id-2", SENT_TO_UPDATESERVICE))));
        assertThat("dpf errors", dpfRecord1.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
        assertThat("dpf catalogueCode", dpfRecord1.getBody().getSubFieldValues("032", 'a'), is(Arrays.asList("PFP201946", "DPF201945", "GPG201945")));
        assertThat("head errors", dpfRecord2.getBody().getSubFieldValues("e99", 'b'), is(Collections.emptyList()));
    }

    private DataField createDataField(String code, List<SubField> subFields) {
        DataField dataField = new DataField(code, "00");
        dataField.addAllSubFields(subFields);

        return dataField;
    }

    private UpdateRecordResult createOKUpdateRecordResult() {
        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.OK);

        return updateRecordResult;
    }

    private UpdateRecordResponseDTO createOKDoubleRecordResult() {
        UpdateRecordResponseDTO updateRecordResult = new UpdateRecordResponseDTO();
        updateRecordResult.setUpdateStatusEnumDTO(UpdateStatusEnumDTO.OK);

        return updateRecordResult;
    }

    private UpdateRecordResponseDTO createDoubleRecordErrorResult(List<String> messageStrings) {
        UpdateRecordResponseDTO updateRecordResult = new UpdateRecordResponseDTO();
        updateRecordResult.setUpdateStatusEnumDTO(UpdateStatusEnumDTO.FAILED);

        List<DoubleRecordFrontendDTO> doubleRecordEntries = new ArrayList<>();
        for (String message : messageStrings) {
            DoubleRecordFrontendDTO entry = new DoubleRecordFrontendDTO();
            entry.setMessage(message);
            doubleRecordEntries.add(entry);
        }

        updateRecordResult.addDoubleRecordFrontendDtos(doubleRecordEntries);
        return updateRecordResult;
    }

    private UpdateRecordResult createErrorUpdateRecordResult(List<String> messageStrings) {
        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);

        Messages messages = new Messages();
        for (String message : messageStrings) {
            MessageEntry messageEntry = new MessageEntry();
            messageEntry.setMessage(message);

            messages.getMessageEntry().add(messageEntry);
        }
        updateRecordResult.setMessages(messages);

        return updateRecordResult;
    }

}
