package dk.dbc.dataio.commons.types.json.mixins;

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
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobInfoTest;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobSpecificationTest;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.SubmitterContentTest;
import dk.dbc.dataio.commons.types.SubmitterTest;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.junit.Test;

public class MixInsTest {
    @Test
    public void verify_jsonMixInForSubmitter() throws Exception {
        final String json = JsonUtil.toJson(SubmitterTest.newSubmitterInstance());
        JsonUtil.fromJson(json, Submitter.class, MixIns.getMixIns());
    }

    @Test
    public void verify_jsonMixInForSubmitterContent() throws Exception {
        final String json = JsonUtil.toJson(SubmitterContentTest.newSubmitterContentInstance());
        JsonUtil.fromJson(json, SubmitterContent.class, MixIns.getMixIns());
    }

    @Test
    public void verify_jsonMixInForJavaScript() throws Exception {
        final String json = JsonUtil.toJson(JavaScriptTest.newJavaScriptInstance());
        JsonUtil.fromJson(json, JavaScript.class, MixIns.getMixIns());
    }

    @Test
    public void verify_jsonMixInForFlow() throws Exception {
        final String json = JsonUtil.toJson(FlowTest.newFlowInstance());
        JsonUtil.fromJson(json, Flow.class, MixIns.getMixIns());
    }

    @Test
    public void verify_jsonMixInForFlowContent() throws Exception {
        final String json = JsonUtil.toJson(FlowContentTest.newFlowContentInstance());
        JsonUtil.fromJson(json, FlowContent.class, MixIns.getMixIns());
    }

    @Test
    public void verify_jsonMixInForFlowComponent() throws Exception {
        final String json = JsonUtil.toJson(FlowComponentTest.newFlowComponentInstance());
        JsonUtil.fromJson(json, FlowComponent.class, MixIns.getMixIns());
    }

    @Test
    public void verify_jsonMixInForFlowComponentContent() throws Exception {
        final String json = JsonUtil.toJson(FlowComponentContentTest.newFlowComponentContentInstance());
        JsonUtil.fromJson(json, FlowComponentContent.class, MixIns.getMixIns());
    }

    @Test
    public void verify_jsonMixInForFlowBinder() throws Exception {
        final String json = JsonUtil.toJson(FlowBinderTest.newFlowBinderInstance());
        JsonUtil.fromJson(json, FlowBinder.class, MixIns.getMixIns());
    }

    @Test
    public void verify_jsonMixInForFlowBinderContent() throws Exception {
        final String json = JsonUtil.toJson(FlowBinderContentTest.newFlowBinderContentInstance());
        JsonUtil.fromJson(json, FlowBinderContent.class, MixIns.getMixIns());
    }

    @Test
    public void verify_jsonMixInForJobInfo() throws Exception {
        final String json = JsonUtil.toJson(JobInfoTest.newJobInfoInstance());
        JsonUtil.fromJson(json, JobInfo.class, MixIns.getMixIns());
    }

    @Test
    public void verify_jsonMixInForJobSpecification() throws Exception {
        final String json = JsonUtil.toJson(JobSpecificationTest.newJobSpecificationInstance());
        JsonUtil.fromJson(json, JobSpecification.class, MixIns.getMixIns());
    }
}
