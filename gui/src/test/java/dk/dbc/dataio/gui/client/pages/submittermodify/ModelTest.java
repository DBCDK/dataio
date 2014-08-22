package dk.dbc.dataio.gui.client.pages.submittermodify;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;

public class ModelTest {

    SubmitterModifyConstants constants = mock(SubmitterModifyConstants.class);

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        Model model = new Model();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getNumber(), is(""));
        assertThat(model.getName(), is(""));
        assertThat(model.getDescription(), is(""));
    }

    @Test
    public void validateNumber_validNumberInput_numberIsValid() {
        Model model = getTestModel();
        try {
            model.validateNumber(constants);
        } catch (NumberFormatException e) {
            fail("Unexpected exception thrown by Model.validateNumber()");
        }
    }

    @Test(expected = NumberFormatException.class)
    public void validateNumber_invalidNumberInput_throws() {
        Model model = getTestModel();
        model.setNumber("InvalidNumber");
        model.validateNumber(constants);
    }

    @Test
    public void isInputFieldsEmpty_emptyNumberInput_returnsTrue() {
        Model model = getTestModel();
        model.setNumber("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyNameInput_returnsTrue() {
        Model model = getTestModel();
        model.setName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyDescriptionInput_returnsTrue() {
        Model model = getTestModel();
        model.setDescription("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_allInputFieldsSet_returnsFalse() {
        Model model = getTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    private Model getTestModel() {
        return new Model(1, 1, "455", "Name", "Description");
    }
}
