package dk.dbc.dataio.gui.client.model;

import org.junit.Test;

import java.util.List;

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

    @Test
    public void getDataioPatternMatches_validSinkNameInput_returnsEmptyList() {
        SinkModel model = getTestModel();
        model.setSinkName("Valid sink name + 1_2_3");
        assertThat(model.getDataioPatternMatches().size(), is(0));
    }

    @Test
    public void getDataioPatternMatches_invalidSinkNameInput_returnsList() {
        final SinkModel model = getTestModel();
        final String expectedInvalidValues = "*<>*(#€)";
        model.setSinkName("Invalid sink name" + expectedInvalidValues);

        final List<String> matches = model.getDataioPatternMatches();
        assertThat(matches.size(), is(expectedInvalidValues.length()));
        for (int i = 0; i < matches.size(); i++) {
            assertThat(matches.get(i), is(String.valueOf(expectedInvalidValues.charAt(i))));
        }
    }

    private SinkModel getTestModel() {
        return new SinkModel(1, 1, "Name", "Resource");
    }
}
