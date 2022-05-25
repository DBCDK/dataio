package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
        model.setJavascriptModules(new ArrayList<>());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_allInputFieldsSet_returnsFalse() {
        FlowComponentModel model = getTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void getDataioPatternMatches_validFlowComponentNameInput_returnsEmptyList() {
        FlowComponentModel model = getTestModel();
        model.setName("Valid flow component name + 1_2_3");
        assertThat(model.getDataioPatternMatches().size(), is(0));
    }

    @Test
    public void getDataioPatternMatches_invalidFlowComponentNameInput_returnsList() {
        final FlowComponentModel model = getTestModel();
        final String expectedInvalidValues = "*<>*(#€)";
        model.setName("Invalid flow component name" + expectedInvalidValues);

        final List<String> matches = model.getDataioPatternMatches();
        assertThat(matches.size(), is(expectedInvalidValues.length()));
        for (int i = 0; i < matches.size(); i++) {
            assertThat(matches.get(i), is(String.valueOf(expectedInvalidValues.charAt(i))));
        }
    }

    private FlowComponentModel getTestModel() {
        List<String> javaScripts = new ArrayList<>();
        javaScripts.add("Javascript");
        return new FlowComponentModelBuilder().
                setId(12).
                setVersion(23).
                setName("Name").
                setSvnProject("SVN Project").
                setSvnRevision("SVN Revision").
                setInvocationJavascript("Invocation Javascript").
                setInvocationMethod("Invocation Method").
                setJavascriptModules(javaScripts).
                setDescription("description").
                build();
    }

}
