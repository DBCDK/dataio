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
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowComponentContentTest;
import dk.dbc.dataio.commons.types.FlowComponentTest;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowContentTest;
import dk.dbc.dataio.commons.types.FlowTest;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.JavaScriptTest;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobSpecificationTest;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.PingResponseTest;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.SinkContentTest;
import dk.dbc.dataio.commons.types.SinkTest;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.SubmitterContentTest;
import dk.dbc.dataio.commons.types.SubmitterTest;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.junit.Test;

public class JsonMarshallingTest {
    @Test
    public void verify_jsonMarshallingForSubmitter() throws Exception {
        final String json = JsonUtil.toJson(SubmitterTest.newSubmitterInstance());
        JsonUtil.fromJson(json, Submitter.class);
    }

    @Test
    public void verify_jsonMarshallingForSubmitterContent() throws Exception {
        final String json = JsonUtil.toJson(SubmitterContentTest.newSubmitterContentInstance());
        JsonUtil.fromJson(json, SubmitterContent.class);
    }

    @Test
    public void verify_jsonMarshallingForJavaScript() throws Exception {
        final String json = JsonUtil.toJson(JavaScriptTest.newJavaScriptInstance());
        JsonUtil.fromJson(json, JavaScript.class);
    }

    @Test
    public void verify_jsonMarshallingForFlow() throws Exception {
        final String json = JsonUtil.toJson(FlowTest.newFlowInstance());
        JsonUtil.fromJson(json, Flow.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowContent() throws Exception {
        final String json = JsonUtil.toJson(FlowContentTest.newFlowContentInstance());
        JsonUtil.fromJson(json, FlowContent.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowComponent() throws Exception {
        final String json = JsonUtil.toJson(FlowComponentTest.newFlowComponentInstance());
        JsonUtil.fromJson(json, FlowComponent.class);
    }
    @Test
        public void verify_jsonMarshallingForFlowComponentContent() throws Exception {
        final String json = JsonUtil.toJson(FlowComponentContentTest.newFlowComponentContentInstance());
        JsonUtil.fromJson(json, FlowComponentContent.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowBinder() throws Exception {
        final String json = JsonUtil.toJson(FlowBinderTest.newFlowBinderInstance());
        JsonUtil.fromJson(json, FlowBinder.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowBinderContent() throws Exception {
        final String json = JsonUtil.toJson(FlowBinderContentTest.newFlowBinderContentInstance());
        JsonUtil.fromJson(json, FlowBinderContent.class);
    }

    @Test
    public void verify_jsonMarshallingForJobSpecification() throws Exception {
        final String json = JsonUtil.toJson(JobSpecificationTest.newJobSpecificationInstance());
        JsonUtil.fromJson(json, JobSpecification.class);
    }

    @Test
    public void verify_jsonMarshallingForSink() throws Exception {
        final String json = JsonUtil.toJson(SinkTest.newSinkInstance());
        JsonUtil.fromJson(json, Sink.class);
    }

    @Test
    public void verify_jsonMarshallingForSinkContent() throws Exception {
        final String json = JsonUtil.toJson(SinkContentTest.newSinkContentInstance());
        JsonUtil.fromJson(json, SinkContent.class);
    }

    @Test
    public void verify_jsonMarshallingForPingResponse() throws Exception {
        final String json = JsonUtil.toJson(PingResponseTest.newPingResponse());
        JsonUtil.fromJson(json, PingResponse.class);
    }

    @Test
    public void verify_jsonMarshallingForChunkItem() throws Exception {
        final String json = JsonUtil.toJson(ChunkItemTest.newChunkItemInstance());
        JsonUtil.fromJson(json, ChunkItem.class);
    }
}
