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

import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FlowModelTest {

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        FlowModel model = new FlowModel();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getFlowName(), is(""));
        assertThat(model.getDescription(), is(""));
        assertThat(model.getTimeOfFlowComponentUpdate(), is(""));
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
        model.setFlowComponents(new ArrayList<>());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_allInputFieldsSet_returnsFalse() {
        FlowModel model = getTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void getDataioPatternMatches_validFlowNameInput_returnsEmptyList() {
        FlowModel model = getTestModel();
        model.setFlowName("Valid flow name + 1_2_3");
        assertThat(model.getDataioPatternMatches().size(), is(0));
    }

    @Test
    public void getDataioPatternMatches_invalidFlowNameInput_returnsList() {
        final FlowModel model = getTestModel();
        final String expectedInvalidValues = "*<>*(#€)";
        model.setFlowName("Invalid flow name" + expectedInvalidValues);

        final List<String> matches = model.getDataioPatternMatches();
        assertThat(matches.size(), is(expectedInvalidValues.length()));
        for (int i = 0; i < matches.size(); i++) {
            assertThat(matches.get(i), is(String.valueOf(expectedInvalidValues.charAt(i))));
        }
    }

    private FlowModel getTestModel() {
        return new FlowModel(11, 22, "Name", "Description", "2016-11-18 15:24:40", Collections.singletonList(new FlowComponentModelBuilder().build()));
    }

}
