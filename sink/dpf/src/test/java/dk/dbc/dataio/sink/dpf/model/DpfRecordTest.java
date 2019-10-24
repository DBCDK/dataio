/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.dataio.sink.dpf.MarcRecordFactory;
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

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DpfRecordTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    private final ProcessingInstructions processingInstructions = new ProcessingInstructions()
            .withSubmitter(870970)
            .withId("test")
            .withRecordState(DpfRecord.State.NEW)
            .withUpdateTemplate("dbcperiodica")
            .withTitle("A title for test")
            .withErrors(Arrays.asList("err1", "err2"));

    private final MarcRecord marcRecord = new MarcRecord()
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

    @Test
    public void toLobbyApplicant() throws JSONBException {
        final Applicant expectedApplicant = new Applicant();
        expectedApplicant.setId("test");
        expectedApplicant.setCategory("bpf");
        expectedApplicant.setMimetype("application/xml");
        expectedApplicant.setBody(MarcRecordFactory.toMarcXchange(marcRecord));
        expectedApplicant.setState(ApplicantState.PENDING);
        expectedApplicant.setAdditionalInfo(processingInstructions);

        final DpfRecord dpfRecord = new DpfRecord(processingInstructions, marcRecord);
        final Applicant applicant = dpfRecord.toLobbyApplicant();
        assertThat("applicant ID", applicant.getId(), is("test"));
        assertThat("applicant category", applicant.getCategory(), is("dpf"));
        assertThat("applicant mimetype", applicant.getMimetype(), is("application/xml"));
        assertThat("applicant state", applicant.getState(), is(ApplicantState.PENDING));
        assertThat("applicant body", applicant.getBody(), is(MarcRecordFactory.toMarcXchange(marcRecord)));
        assertThat("applicant additional info", applicant.getAdditionalInfo().toString(),
                is(jsonbContext.marshall(processingInstructions)));
    }
}