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

package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * SubmitterModelMapper unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SubmitterModelMapperTest {
    // Default Submitters
    private static final SubmitterContent defaultSubmitterContent1 = new SubmitterContentBuilder().setNumber(1111L).setName("submitter content name 1").setDescription("submitter content description 1").build();
    private static final SubmitterContent defaultSubmitterContent2 = new SubmitterContentBuilder().setNumber(2222L).setName("submitter content name 2").setDescription("submitter content description 2").build();
    private static final Submitter defaultSubmitter1 = new Submitter(3333L, 4444L, defaultSubmitterContent1);
    private static final Submitter defaultSubmitter2 = new Submitter(5555L, 6666L, defaultSubmitterContent2);
    private static final List<Submitter> defaultSubmitterList = Arrays.asList(defaultSubmitter1, defaultSubmitter2);

    // Default SubmitterModels
    private static final SubmitterModel defaultSubmitterModel1 = new SubmitterModelBuilder().build();


    @Test(expected = NullPointerException.class)
    public void toModel_nullInput_throws() {
        // Activate Subject Under Test
        SubmitterModelMapper.toModel(null);
    }

    @Test
    public void toModel_validInput_returnsValidModel() {
        // Activate Subject Under Test
        SubmitterModel model = SubmitterModelMapper.toModel(defaultSubmitter1);

        // Verification
        assertThat(model.getId(), is(defaultSubmitter1.getId()));
        assertThat(model.getVersion(), is(defaultSubmitter1.getVersion()));
        assertThat(model.getNumber(), is(String.valueOf(defaultSubmitter1.getContent().getNumber())));
        assertThat(model.getName(), is(defaultSubmitter1.getContent().getName()));
        assertThat(model.getDescription(), is(defaultSubmitter1.getContent().getDescription()));
    }

    @Test(expected = NullPointerException.class)
    public void toSubmitterContent_nullInput_throws() {
        // Activate Subject Under Test
        SubmitterModelMapper.toSubmitterContent(null);
    }

    @Test
    public void toSubmitterContent_validInput_returnsValidSubmitterContent() {
        // Activate Subject Under Test
        SubmitterContent submitterContent = SubmitterModelMapper.toSubmitterContent(defaultSubmitterModel1);

        // Verification
        assertThat(submitterContent.getNumber(), is(Long.parseLong(defaultSubmitterModel1.getNumber())));
        assertThat(submitterContent.getName(), is(defaultSubmitterModel1.getName()));
        assertThat(submitterContent.getDescription(), is(defaultSubmitterModel1.getDescription()));
    }

    @Test(expected = NullPointerException.class)
    public void toListOfSubmitterModels_nullInput_throws() {
        // Activate Subject Under Test
        SubmitterModelMapper.toListOfSubmitterModels(null);
    }

    @Test
    public void toListOfSubmitterModels_emptyInputList_returnsEmptyListOfSubmitterModels() {
        // Activate Subject Under Test
        List<SubmitterModel> submitterModels = SubmitterModelMapper.toListOfSubmitterModels(new ArrayList<Submitter>());

        // Verification
        assertThat(submitterModels.size(), is(0));
    }

    @Test
    public void toListOfSubmitterModels_validListOfSubmitters_returnsValidListOfSubmitterModels() {
        // Activate Subject Under Test
        List<SubmitterModel> submitterModels = SubmitterModelMapper.toListOfSubmitterModels(defaultSubmitterList);

        // Verification
        assertThat(submitterModels.size(), is(2));
        assertThat(submitterModels.get(0).getId(), is(3333L));
        assertThat(submitterModels.get(1).getId(), is(5555L));
    }

    @Test
    public void toSubmitterContent_invalidSubmitterName_throwsIllegalArgumentException() {
        final String submitterName = "*%(Illegal)_&Name - €";
        final String expectedIllegalCharacters = "[*], [%], [(], [)], [&], [€]";
        SubmitterModel model = defaultSubmitterModel1;
        model.setName(submitterName);
        try {
            SubmitterModelMapper.toSubmitterContent(model);
            fail("Illegal submitter name not detected");
        } catch(IllegalArgumentException e) {
            assertThat(e.getMessage().contains(expectedIllegalCharacters), is (true));
        }
    }

}
