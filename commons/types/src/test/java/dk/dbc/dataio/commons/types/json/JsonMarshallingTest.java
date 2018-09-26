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

package dk.dbc.dataio.commons.types.json;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkItemTest;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowBinderContentTest;
import dk.dbc.dataio.commons.types.FlowBinderTest;
import dk.dbc.dataio.commons.types.FlowBinderWithSubmitter;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowComponentContentTest;
import dk.dbc.dataio.commons.types.FlowComponentTest;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowContentTest;
import dk.dbc.dataio.commons.types.FlowTest;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.types.GatekeeperDestinationTest;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.JavaScriptTest;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobSpecificationTest;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.SinkContentTest;
import dk.dbc.dataio.commons.types.SinkTest;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.SubmitterContentTest;
import dk.dbc.dataio.commons.types.SubmitterTest;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JsonMarshallingTest {

    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void verify_jsonMarshallingForSubmitter() throws Exception {
        final String json = jsonbContext.marshall(SubmitterTest.newSubmitterInstance());
        jsonbContext.unmarshall(json, Submitter.class);
    }

    @Test
    public void verify_jsonMarshallingForSubmitterContent() throws Exception {
        final String json = jsonbContext.marshall(SubmitterContentTest.newSubmitterContentInstance());
        jsonbContext.unmarshall(json, SubmitterContent.class);
    }

    @Test
    public void verify_jsonMarshallingForJavaScript() throws Exception {
        final String json = jsonbContext.marshall(JavaScriptTest.newJavaScriptInstance());
        jsonbContext.unmarshall(json, JavaScript.class);
    }

    @Test
    public void verify_jsonMarshallingForFlow() throws Exception {
        final String json = jsonbContext.marshall(FlowTest.newFlowInstance());
        jsonbContext.unmarshall(json, Flow.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowContent() throws Exception {
        final String json = jsonbContext.marshall(FlowContentTest.newFlowContentInstance().withTimeOfFlowComponentUpdate(new Date()));
        jsonbContext.unmarshall(json, FlowContent.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowContentWithoutTimeOfFlowComponentUpdate() throws Exception {
        final String json = jsonbContext.marshall(FlowContentTest.newFlowContentInstance());
        jsonbContext.unmarshall(json, FlowContent.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowComponent() throws Exception {
        final String json = jsonbContext.marshall(FlowComponentTest.newFlowComponentInstance());
        jsonbContext.unmarshall(json, FlowComponent.class);
    }
    @Test
        public void verify_jsonMarshallingForFlowComponentContent() throws Exception {
        final String json = jsonbContext.marshall(FlowComponentContentTest.newFlowComponentContentInstance());
        jsonbContext.unmarshall(json, FlowComponentContent.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowBinder() throws Exception {
        final String json = jsonbContext.marshall(FlowBinderTest.newFlowBinderInstance());
        jsonbContext.unmarshall(json, FlowBinder.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowBinderContent() throws Exception {
        final String json = jsonbContext.marshall(FlowBinderContentTest.newFlowBinderContentInstance());
        jsonbContext.unmarshall(json, FlowBinderContent.class);
    }

    @Test
    public void verify_jsonMarshallingForJobSpecification() throws Exception {
        final String json = jsonbContext.marshall(JobSpecificationTest.newJobSpecificationInstance());
        jsonbContext.unmarshall(json, JobSpecification.class);
    }

    @Test
    public void verify_jsonMarshallingForSink() throws Exception {
        final String json = jsonbContext.marshall(SinkTest.newSinkInstance());
        jsonbContext.unmarshall(json, Sink.class);
    }

    @Test
    public void verify_jsonMarshallingForSinkContent() throws Exception {
        final String json = jsonbContext.marshall(SinkContentTest.newSinkContentInstance());
        jsonbContext.unmarshall(json, SinkContent.class);
    }

    @Test
    public void verify_jsonMarshallingForSinkContentWithType() throws Exception {
        final String json = jsonbContext.marshall(SinkContentTest.newSinkContentWithTypeInstance());
        jsonbContext.unmarshall(json, SinkContent.class);
    }

    @Test
    public void verify_jsonMarshallingForSinkContentWithTypeAndConfig() throws Exception {
        final String json = jsonbContext.marshall(SinkContentTest.newSinkContentWithTypeAndConfigInstance());
        jsonbContext.unmarshall(json, SinkContent.class);
    }

    @Test
    public void verify_jsonMarshallingForChunkItem() throws Exception {
        final String json = jsonbContext.marshall(ChunkItemTest.newChunkItemInstance());
        jsonbContext.unmarshall(json, ChunkItem.class);
    }

    @Test
    public void verify_jsonMarshallingForChunkItemWithTypeAndEncoding() throws Exception {
        final String json = jsonbContext.marshall(ChunkItem.successfulChunkItem()
                                                    .withId(42)
                                                    .withData("data")
                                                    .withType(ChunkItem.Type.UNKNOWN)
                                                    .withEncoding(StandardCharsets.UTF_8));
        jsonbContext.unmarshall(json, ChunkItem.class);
    }

    @Test
    public void verify_jsonMarshallingForSavedGatekeeperDestination() throws Exception {
        final String json = jsonbContext.marshall(GatekeeperDestinationTest.newGatekeeperDestinationInstance());
        jsonbContext.unmarshall(json, GatekeeperDestination.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowBinderWithSubmitter() throws JSONBException {
        final FlowBinderWithSubmitter flowBinderWithSubmitter =
                new FlowBinderWithSubmitter("name", 1L, 2L);
        final String json = jsonbContext.marshall(flowBinderWithSubmitter);
        jsonbContext.unmarshall(json, FlowBinderWithSubmitter.class);
    }
}
