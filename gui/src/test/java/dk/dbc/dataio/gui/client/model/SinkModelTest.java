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

import dk.dbc.dataio.commons.types.SinkContent;
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
        assertThat(model.getSinkType(), is(SinkContent.SinkType.ES));
        assertThat(model.getSinkName(), is(""));
        assertThat(model.getResourceName(), is(""));
        assertThat(model.getDescription(), is(""));
        assertThat(model.getOpenUpdateUserId(), is(""));
        assertThat(model.getOpenUpdatePassword(), is(""));
        assertThat(model.getOpenUpdateEndpoint(), is(""));
    }

    @Test
    public void createModel_oldStyleArgs_returnsNewInstanceWithDefaultValues() {
        SinkModel model = new SinkModel(1L, 2L, "nam", "resou", "descri");
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(1L));
        assertThat(model.getVersion(), is(2L));
        assertThat(model.getSinkType(), is(SinkContent.SinkType.ES));
        assertThat(model.getSinkName(), is("nam"));
        assertThat(model.getResourceName(), is("resou"));
        assertThat(model.getDescription(), is("descri"));
        assertThat(model.getOpenUpdateUserId(), is(""));
        assertThat(model.getOpenUpdatePassword(), is(""));
        assertThat(model.getOpenUpdateEndpoint(), is(""));
    }

    @Test
    public void createModel_nonOpenUpdateStyleArgs_returnsNewInstanceWithDefaultValues() {
        SinkModel model = new SinkModel(3L, 4L, SinkContent.SinkType.FBS, "nam2", "resou2", "descri2");
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(3L));
        assertThat(model.getVersion(), is(4L));
        assertThat(model.getSinkType(), is(SinkContent.SinkType.FBS));
        assertThat(model.getSinkName(), is("nam2"));
        assertThat(model.getResourceName(), is("resou2"));
        assertThat(model.getDescription(), is("descri2"));
        assertThat(model.getOpenUpdateUserId(), is(""));
        assertThat(model.getOpenUpdatePassword(), is(""));
        assertThat(model.getOpenUpdateEndpoint(), is(""));
    }

    @Test
    public void createModel_allArgs_returnsNewInstanceWithDefaultValues() {
        SinkModel model = new SinkModel(5L, 6L, SinkContent.SinkType.OPENUPDATE, "nam3", "resou3", "descri3", "user", "pass", "url");
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
    public void isInputFieldsEmpty_oldStyleEmptySinkNameInput_returnsTrue() {
        SinkModel model = getOldStyleTestModel();
        model.setSinkName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_oldStyleEmptyResourceNameInput_returnsTrue() {
        SinkModel model = getOldStyleTestModel();
        model.setResourceName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_oldStyleEmptyDescriptionInput_returnsFalse() {
        SinkModel model = getOldStyleTestModel();
        model.setDescription("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_oldStyleAllInputFieldsSet_returnsFalse() {
        SinkModel model = getOldStyleTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_newStyleESEmptyUserId_returnsFalse() {
        SinkModel model = getNewStyleESTestModel();
        model.setOpenUpdateUserId("");  // Does not cause input fields to be empty due to ES type
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_newStyleESEmptyPassword_returnsFalse() {
        SinkModel model = getNewStyleESTestModel();
        model.setOpenUpdatePassword("");  // Does not cause input fields to be empty due to ES type
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_newStyleESEmptyEndpoint_returnsFalse() {
        SinkModel model = getNewStyleESTestModel();
        model.setOpenUpdateEndpoint("");  // Does not cause input fields to be empty due to ES type
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_newStyleESAllInputFieldsSet_returnsFalse() {
        SinkModel model = getNewStyleESTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_newStyleOpenUpdateEmptyUserId_returnsTrue() {
        SinkModel model = getNewStyleOpenUpdateTestModel();
        model.setOpenUpdateUserId("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_newStyleOpenUpdateEmptyPassword_returnsTrue() {
        SinkModel model = getNewStyleOpenUpdateTestModel();
        model.setOpenUpdatePassword("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_newStyleOpenUpdateEmptyEndpoint_returnsTrue() {
        SinkModel model = getNewStyleOpenUpdateTestModel();
        model.setOpenUpdateEndpoint("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_newStyleOpenUpdateAllInputFieldsSet_returnsFalse() {
        SinkModel model = getNewStyleOpenUpdateTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void getDataioPatternMatches_validSinkNameInput_returnsEmptyList() {
        SinkModel model = getOldStyleTestModel();
        model.setSinkName("Valid sink name + 1_2_3");
        assertThat(model.getDataioPatternMatches().size(), is(0));
    }

    @Test
    public void getDataioPatternMatches_invalidSinkNameInput_returnsList() {
        final SinkModel model = getOldStyleTestModel();
        final String expectedInvalidValues = "*<>*(#€)";
        model.setSinkName("Invalid sink name" + expectedInvalidValues);

        final List<String> matches = model.getDataioPatternMatches();
        assertThat(matches.size(), is(expectedInvalidValues.length()));
        for (int i = 0; i < matches.size(); i++) {
            assertThat(matches.get(i), is(String.valueOf(expectedInvalidValues.charAt(i))));
        }
    }

    private SinkModel getOldStyleTestModel() {
        return new SinkModel(1, 2, "Name", "Resource", "Description");
    }

    private SinkModel getNewStyleESTestModel() {
        return new SinkModel(3, 4, SinkContent.SinkType.ES, "Name1", "Resource1", "Description1");
    }

    private SinkModel getNewStyleOpenUpdateTestModel() {
        return new SinkModel(5, 6, SinkContent.SinkType.OPENUPDATE, "Name2", "Resource2", "Description2", "User2", "Pass2", "Endpoint2");
    }

}
