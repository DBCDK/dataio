/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.json;


import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.DiagnosticBuilder;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferenceBuilder;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.test.types.ItemInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.test.types.WorkflowNoteBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import org.junit.Test;

import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class JsonMarshallingTest {

    final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void verify_jsonMarshallingForJobInfoSnapshot() throws Exception {
        final String json = jsonbContext.marshall(new JobInfoSnapshot().withFatalError(true));
        final JobInfoSnapshot jobInfoSnapshot = jsonbContext.unmarshall(json, JobInfoSnapshot.class);
        assertThat(jobInfoSnapshot.hasFatalError(), is(true)); // Extra test on fatal error because of @JsonProperty
    }

    @Test
    public void verify_jsonMarshallingForItemInfoSnapshot() throws Exception {
        final String json = jsonbContext.marshall(new ItemInfoSnapshotBuilder().build());
        jsonbContext.unmarshall(json, ItemInfoSnapshot.class);
    }

    @Test
    public void verify_jsonMarshallingForDiagnostic() throws Exception {
        final String json = jsonbContext.marshall(new DiagnosticBuilder().build());
        jsonbContext.unmarshall(json, Diagnostic.class);
    }

    @Test
    public void verify_jsonMarshallingForState() throws Exception {
        final String json = jsonbContext.marshall(new State());
        jsonbContext.unmarshall(json, State.class);
    }

    @Test
    public void verify_jsonMarshallingForSequenceAnalysisData() throws Exception {
        final String json = jsonbContext.marshall(new SequenceAnalysisData(new HashSet<>()));
        jsonbContext.unmarshall(json, SequenceAnalysisData.class);
    }

    @Test
    public void verify_jsonMarshallingForJobInputStream() throws Exception {
        final String json = jsonbContext.marshall(new JobInputStream(new JobSpecification(), true, 123456));
        jsonbContext.unmarshall(json, JobInputStream.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowStoreReference() throws Exception {
        final String json = jsonbContext.marshall(new FlowStoreReferenceBuilder().build());
        jsonbContext.unmarshall(json, FlowStoreReference.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowStoreReferences() throws Exception {
        final String json = jsonbContext.marshall(new FlowStoreReferencesBuilder().build());
        jsonbContext.unmarshall(json, FlowStoreReferences.class);
    }

    @Test
    public void verify_jsonMarshallingForWorkflowNote() throws Exception {
        final String json = jsonbContext.marshall(new WorkflowNoteBuilder().build());
        jsonbContext.unmarshall(json, WorkflowNote.class);
    }

    @Test
    public void verify_jsonMarshallingForNullValuedWorkflowNote() throws Exception {
        final String json = jsonbContext.marshall(null);
        jsonbContext.unmarshall(json, WorkflowNote.class);
    }

    @Test
    public void verify_jsonMarshallingForJobError() throws Exception {
        final String json = jsonbContext.marshall(new JobError(JobError.Code.ILLEGAL_CHUNK, "description", "stacktrace"));
        jsonbContext.unmarshall(json, JobError.class);
    }
}
