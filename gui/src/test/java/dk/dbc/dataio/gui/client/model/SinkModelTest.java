/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import static org.junit.Assert.assertThat;

public class SinkModelTest {

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        SinkModel model = new SinkModel();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getSinkType(), is(SinkContent.SinkType.ES));
        assertThat(model.getSinkName(), is(""));
        assertThat(model.getResourceName(), is(""));
        assertThat(model.getDescription(), is(""));
        assertThat(model.getSinkConfig(), is(nullValue()));
    }

    @Test
    public void constructor_withConfigValues_returnsNewInstanceWithDefaultValues() {
        SinkModel model = new SinkModel(5L, 6L, SinkContent.SinkType.OPENUPDATE, "nam3", "resou3", "descri3", SinkContent.SequenceAnalysisOption.ALL,
                new OpenUpdateSinkConfig().withUserId("user").withPassword("pass").withEndpoint("url"));

        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(5L));
        assertThat(model.getVersion(), is(6L));
        assertThat(model.getSinkType(), is(SinkContent.SinkType.OPENUPDATE));
        assertThat(model.getSinkName(), is("nam3"));
        assertThat(model.getResourceName(), is("resou3"));
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
    public void isInputFieldsEmpty_noConfigEmptyResourceNameInput_returnsTrue() {
        SinkModel model = getNoConfigTestModel();
        model.setResourceName("");
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
        return new SinkModel(1, 2, SinkContent.SinkType.DUMMY, "Name", "Resource", "Description", SinkContent.SequenceAnalysisOption.ALL, null);
    }

    private SinkModel getWithConfigTestModel() {
        return new SinkModel(5, 6, SinkContent.SinkType.OPENUPDATE, "Name2", "Resource2", "Description2", SinkContent.SequenceAnalysisOption.ALL,
                new OpenUpdateSinkConfig());
    }

}
