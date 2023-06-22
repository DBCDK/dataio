package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.dataio.sink.dpf.transform.MarcRecordFactory;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.lobby.Applicant;
import dk.dbc.lobby.ApplicantState;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Leader;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.reader.DanMarc2LineFormatReader;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DpfRecordTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void hasErrors() {
        assertThat("with errors", new DpfRecord(newProcessingInstructions()
                        .withErrors(Collections.singletonList("error")), new MarcRecord()).hasErrors(),
                is(true));
        assertThat("without errors", new DpfRecord(
                        newProcessingInstructions(), new MarcRecord()).hasErrors(),
                is(false));
    }

    @Test
    public void toLobbyApplicant() throws JSONBException {
        final ProcessingInstructions expectedProcessingInstructions = newProcessingInstructions()
                .withErrors(Arrays.asList("err1", "err2"));
        final MarcRecord expectedMarcRecord = newMarcRecord()
                .addField(new DataField("e99", "00")
                        .addSubField(new SubField()
                                .setCode('b').setData("err1")))
                .addField(new DataField("e99", "00")
                        .addSubField(new SubField()
                                .setCode('b').setData("err2")));

        final Applicant expectedApplicant = new Applicant();
        expectedApplicant.setId("test");
        expectedApplicant.setCategory("bpf");
        expectedApplicant.setMimetype("application/xml");
        expectedApplicant.setBody(MarcRecordFactory.toMarcXchange(expectedMarcRecord));
        expectedApplicant.setState(ApplicantState.PENDING);
        expectedApplicant.setAdditionalInfo(expectedProcessingInstructions);

        final DpfRecord dpfRecord = new DpfRecord(newProcessingInstructions()
                .withErrors(Arrays.asList("err1", "err2")), newMarcRecord());
        final Applicant applicant = dpfRecord.toLobbyApplicant();
        assertThat("applicant ID", applicant.getId(), is("test"));
        assertThat("applicant category", applicant.getCategory(), is("dpf"));
        assertThat("applicant mimetype", applicant.getMimetype(), is("application/xml"));
        assertThat("applicant state", applicant.getState(), is(ApplicantState.PENDING));
        assertThat("applicant body", new String(applicant.getBody(), StandardCharsets.UTF_8),
                is(new String(MarcRecordFactory.toMarcXchange(expectedMarcRecord), StandardCharsets.UTF_8)));
        assertThat("applicant additional info", applicant.getAdditionalInfo().toString(),
                is(jsonbContext.marshall(expectedProcessingInstructions)));
    }

    @Test
    public void addError() {
        final ProcessingInstructions expectedProcessingInstructions = newProcessingInstructions()
                .withErrors(Collections.singletonList("some error"));
        final MarcRecord expectedMarcRecord = newMarcRecord()
                .addField(new DataField("e99", "00")
                        .addSubField(new SubField()
                                .setCode('b').setData("some error")));

        final DpfRecord dpfRecord = new DpfRecord(newProcessingInstructions(), newMarcRecord());
        dpfRecord.addError("some error");
        assertThat("DPF record processing instructions", dpfRecord.getProcessingInstructions(),
                is(expectedProcessingInstructions));
        assertThat("DPF record body", dpfRecord.getBody(),
                is(expectedMarcRecord));
    }

    private ProcessingInstructions newProcessingInstructions() {
        return new ProcessingInstructions()
                .withSubmitter(870970)
                .withId("test")
                .withRecordState(DpfRecord.State.NEW)
                .withUpdateTemplate("dbcperiodica")
                .withTitle("A title for test")
                .withErrors(new ArrayList<>());
    }

    private MarcRecord newMarcRecord() {
        return new MarcRecord()
                .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
                .addField(
                        new DataField()
                                .setTag("001")
                                .setInd1('0')
                                .setInd2('0')
                                .addSubfield(new SubField()
                                        .setCode('a')
                                        .setData("123456"))
                                .addSubfield(new SubField()
                                        .setCode('b')
                                        .setData("870970")));
    }
}
