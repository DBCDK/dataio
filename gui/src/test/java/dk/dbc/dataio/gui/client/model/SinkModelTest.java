package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SinkModelTest {

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        SinkModel model = new SinkModel();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getSinkType(), is(SinkContent.SinkType.ES));
        assertThat(model.getSinkName(), is(""));
        assertThat(model.getQueue(), is(""));
        assertThat(model.getDescription(), is(""));
        assertThat(model.getSinkConfig(), is(nullValue()));
    }

    @Test
    public void constructor_withConfigValues_returnsNewInstanceWithDefaultValues() {
        SinkModel model = new SinkModel(5L, 6L, SinkContent.SinkType.OPENUPDATE, "nam3", "queue", "descri3", SinkContent.SequenceAnalysisOption.ALL,
                new OpenUpdateSinkConfig().withUserId("user").withPassword("pass").withEndpoint("url"), 1);

        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(5L));
        assertThat(model.getVersion(), is(6L));
        assertThat(model.getSinkType(), is(SinkContent.SinkType.OPENUPDATE));
        assertThat(model.getSinkName(), is("nam3"));
        assertThat(model.getQueue(), is("queue"));
        assertThat(model.getDescription(), is("descri3"));
        assertThat(model.getOpenUpdateUserId(), is("user"));
        assertThat(model.getOpenUpdatePassword(), is("pass"));
        assertThat(model.getOpenUpdateEndpoint(), is("url"));
    }

    @Test
    public void isInputFieldsEmpty_noConfigEmptySinkNameInput_returnsTrue() {
        SinkModel model = getNoConfigTestModel();
        model.setSinkName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_noConfigEmptyDescriptionInput_returnsFalse() {
        SinkModel model = getNoConfigTestModel();
        model.setDescription("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_noConfigAllInputFieldsSet_returnsFalse() {
        SinkModel model = getNoConfigTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_openUpdateUserIdIsNull_returnsTrue() {
        SinkModel model = getWithConfigTestModel();
        model.setOpenUpdatePassword("pass");
        model.setOpenUpdateEndpoint("endpoint");
        model.setOpenUpdateAvailableQueueProviders(new ArrayList<>());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_openUpdatePasswordIsNull_returnsTrue() {
        SinkModel model = getWithConfigTestModel();
        model.setOpenUpdateUserId("user");
        model.setOpenUpdateEndpoint("endpoint");
        model.setOpenUpdateAvailableQueueProviders(new ArrayList<>());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_openUpdateEndpointIsNull_returnsTrue() {
        SinkModel model = getWithConfigTestModel();
        model.setOpenUpdateUserId("user");
        model.setOpenUpdatePassword("pass");
        model.setOpenUpdateAvailableQueueProviders(new ArrayList<>());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_openUpdateAvailableQueueProvidersIsNull_returnsTrue() {
        SinkModel model = getWithConfigTestModel();
        model.setOpenUpdateUserId("user");
        model.setOpenUpdatePassword("pass");
        model.setOpenUpdateEndpoint("endpoint");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_openUpdateAllInputFieldsSet_returnsFalse() {
        SinkModel model = getWithConfigTestModel();
        model.setOpenUpdateUserId("user");
        model.setOpenUpdatePassword("pass");
        model.setOpenUpdateEndpoint("endpoint");
        model.setOpenUpdateAvailableQueueProviders(Collections.singletonList("avail"));
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void getDataioPatternMatches_validSinkNameInput_returnsEmptyList() {
        SinkModel model = getNoConfigTestModel();
        model.setSinkName("Valid sink name + 1_2_3");
        assertThat(model.getDataioPatternMatches().size(), is(0));
    }

    @Test
    public void getDataioPatternMatches_invalidSinkNameInput_returnsList() {
        final SinkModel model = getNoConfigTestModel();
        final String expectedInvalidValues = "*<>*(#€)";
        model.setSinkName("Invalid sink name" + expectedInvalidValues);

        final List<String> matches = model.getDataioPatternMatches();
        assertThat(matches.size(), is(expectedInvalidValues.length()));
        for (int i = 0; i < matches.size(); i++) {
            assertThat(matches.get(i), is(String.valueOf(expectedInvalidValues.charAt(i))));
        }
    }

    private SinkModel getNoConfigTestModel() {
        return new SinkModel(1, 2, SinkContent.SinkType.DUMMY, "Name", "Queue", "Description", SinkContent.SequenceAnalysisOption.ALL, null, 1);
    }

    private SinkModel getWithConfigTestModel() {
        return new SinkModel(5, 6, SinkContent.SinkType.OPENUPDATE, "Name2", "Queue", "Description2", SinkContent.SequenceAnalysisOption.ALL,
                new OpenUpdateSinkConfig(), 1);
    }

}
