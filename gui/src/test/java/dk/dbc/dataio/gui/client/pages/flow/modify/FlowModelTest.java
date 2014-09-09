package dk.dbc.dataio.gui.client.pages.flow.modify;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class FlowModelTest {

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        FlowModel model = new FlowModel();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getFlowName(), is(""));
        assertThat(model.getDescription(), is(""));
        assertThat(model.getFlowComponents().size(), is(0));
    }

    @Test
    public void isInputFieldsEmpty_emptyFlowNameInput_returnsTrue() {
        FlowModel model = getTestModel();
        model.setFlowName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyDescriptionInput_returnsTrue() {
        FlowModel model = getTestModel();
        model.setDescription("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyFlowDescriptionMapInput_returnsTrue() {
        FlowModel model = getTestModel();
        model.setFlowComponents(new HashMap<String, String>());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_allInputFieldsSet_returnsFalse() {
        FlowModel model = getTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    private FlowModel getTestModel() {
        Map<String, String> flowComponents = new HashMap<String, String>();
        flowComponents.put("12", "Flowcomponent");
        return new FlowModel(11, 22, "Name", "Description", flowComponents);
    }

}
