package dk.dbc.dataio.gui.client.pages.submitter.submittermodify;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SubmitterModelTest {

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        SubmitterModel model = new SubmitterModel();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getNumber(), is(""));
        assertThat(model.getName(), is(""));
        assertThat(model.getDescription(), is(""));
    }

    @Test
    public void validateNumber_validNumberInput_returnsTrue() {
        SubmitterModel model = getTestModel();
        assertThat( model.isNumberValid(), is(true));
    }

    @Test
    public void validateNumber_invalidNumberInput_returnsFalse() {
        SubmitterModel model = getTestModel();
        model.setNumber("InvalidNumber");
        assertThat( model.isNumberValid(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_emptyNumberInput_returnsTrue() {
        SubmitterModel model = getTestModel();
        model.setNumber("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyNameInput_returnsTrue() {
        SubmitterModel model = getTestModel();
        model.setName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyDescriptionInput_returnsTrue() {
        SubmitterModel model = getTestModel();
        model.setDescription("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_allInputFieldsSet_returnsFalse() {
        SubmitterModel model = getTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    private SubmitterModel getTestModel() {
        return new SubmitterModel(1, 1, "455", "Name", "Description");
    }
}
