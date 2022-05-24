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

import dk.dbc.dataio.commons.types.Priority;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(model.getPriority(), is(nullValue()));
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

    @Test
    public void getDataioPatternMatches_validSubmitterNameInput_returnsEmptyList() {
        SubmitterModel model = getTestModel();
        model.setName("Valid flow name + 1_2_3");
        assertThat(model.getDataioPatternMatches().size(), is(0));
    }

    @Test
    public void getDataioPatternMatches_invalidSubmitterNameInput_returnsList() {
        final SubmitterModel model = getTestModel();
        final String expectedInvalidValues = "*<>*(#€)";
        model.setName("Invalid submitter name" + expectedInvalidValues);

        final List<String> matches = model.getDataioPatternMatches();
        assertThat(matches.size(), is(expectedInvalidValues.length()));
        for (int i = 0; i < matches.size(); i++) {
            assertThat(matches.get(i), is(String.valueOf(expectedInvalidValues.charAt(i))));
        }
    }

    private SubmitterModel getTestModel() {
        return new SubmitterModel(1, 1, "455", "Name", "Description", Priority.NORMAL.getValue(), true);
    }
}
