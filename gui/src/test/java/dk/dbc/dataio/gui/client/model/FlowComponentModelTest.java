package dk.dbc.dataio.gui.client.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class FlowComponentModelTest {

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        FlowComponentModel model = new FlowComponentModel();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getName(), is(""));
        assertThat(model.getSvnRevision(), is(""));
        assertThat(model.getInvocationJavascript(), is(""));
        assertThat(model.getInvocationMethod(), is(""));
        assertThat(model.getJavascriptModules().size(), is(0));
    }

    @Test
    public void isInputFieldsEmpty_emptyNameInput_returnsTrue() {
        FlowComponentModel model = getTestModel();
        model.setName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptySvnProjectInput_returnsTrue() {
        FlowComponentModel model = getTestModel();
        model.setSvnProject("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptySvnRevisionInput_returnsTrue() {
        FlowComponentModel model = getTestModel();
        model.setSvnRevision("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyInvocationJavascriptInput_returnsTrue() {
        FlowComponentModel model = getTestModel();
        model.setInvocationJavascript("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyInvocationMethodInput_returnsTrue() {
        FlowComponentModel model = getTestModel();
        model.setInvocationMethod("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyJavaScriptModulesInput_returnsTrue() {
        FlowComponentModel model = getTestModel();
        model.setJavascriptModules(new ArrayList<String>());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_allInputFieldsSet_returnsFalse() {
        FlowComponentModel model = getTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    private FlowComponentModel getTestModel() {
        List<String> javaScripts = new ArrayList<String>();
        javaScripts.add("Javascript");
        return new FlowComponentModel(12, 23, "Name", "SVN Project", "SVN Revision", "Invocation Javascript", "Invocation Method", javaScripts);
    }

}
