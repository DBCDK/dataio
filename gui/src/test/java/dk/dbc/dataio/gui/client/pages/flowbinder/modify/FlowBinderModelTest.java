package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.SubmitterModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class FlowBinderModelTest {

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        FlowBinderModel model = new FlowBinderModel();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getName(), is(""));
        assertThat(model.getDescription(), is(""));
        assertThat(model.getPackaging(), is(""));
        assertThat(model.getFormat(), is(""));
        assertThat(model.getCharset(), is(""));
        assertThat(model.getDestination(), is(""));
        assertThat(model.getRecordSplitter(), is(""));
        assertThat(model.getFlowModel().getFlowName(), is(""));  // FlowModel has been tested, therefore only name is checked
        assertThat(model.getSubmitterModels().size(), is(0));
        assertThat(model.getSinkModel().getSinkName(), is(""));  // FlowModel has been tested, therefore only name is checked
    }

    @Test
    public void isInputFieldsEmpty_emptyFlowNameInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyDescriptionInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setDescription("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyPackagingInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setPackaging("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyFormatInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setFormat("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyCharsetInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setCharset("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyDestinationInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setDestination("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyRecordSplitterInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setRecordSplitter("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_nullFlowModelInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setFlowModel(null);
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyFlowModelInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setFlowModel(new FlowModel());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_nullSubmitterModelsInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setSubmitterModels(null);
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptySubmitterModelsInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setSubmitterModels(new ArrayList<SubmitterModel>());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_nullSinkModelInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setSinkModel(null);
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptySinkModelInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setSinkModel(new SinkModel());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_allInputFieldsSet_returnsFalse() {
        FlowBinderModel model = getTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    private FlowBinderModel getTestModel() {
        FlowComponentModel flowComponentModel = new FlowComponentModel(55L, 66L, "Nam", "Pro", "Rev", "Inv", "Met", Arrays.asList("Script"));
        FlowModel flowModel = new FlowModel(33L, 44L, "Nmm", "Des", Arrays.asList(flowComponentModel));
        SubmitterModel submitterModel = new SubmitterModel(77L, 88L, "Num", "Nim", "Dis");
        SinkModel sinkModel = new SinkModel(99L, 100L, "Snm", "Rsc");
        return new FlowBinderModel(11, 22, "Name", "Description", "Packaging", "Format", "Charset", "Destination", "Record Splitter", flowModel, Arrays.asList(submitterModel), sinkModel);
    }

}
