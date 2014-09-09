package dk.dbc.dataio.gui.client.pages.sink.modify;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SinkModelTest {

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        SinkModel model = new SinkModel();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getSinkName(), is(""));
        assertThat(model.getResourceName(), is(""));
    }

    @Test
    public void isInputFieldsEmpty_emptySinkNameInput_returnsTrue() {
        SinkModel model = getTestModel();
        model.setSinkName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyResourceNameInput_returnsTrue() {
        SinkModel model = getTestModel();
        model.setResourceName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_allInputFieldsSet_returnsFalse() {
        SinkModel model = getTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    private SinkModel getTestModel() {
        return new SinkModel(1, 1, "Name", "Resource");
    }
}
