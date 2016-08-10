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

import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * SinkModelMapper unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SinkModelMapperTest {
    // Default Sinks
    private static final SinkContent defaultSinkContent1 = new SinkContentBuilder().setName("sink content 1").setResource("sink resource 1").build();
    private static final SinkContent defaultSinkContent2 = new SinkContentBuilder().setName("sink content 2").setResource("sink resource 2").build();
    private static final SinkConfig updateSC = new OpenUpdateSinkConfig().withUserId("uid").withPassword("pwd").withEndpoint("url").withAvailableQueueProviders(Collections.singletonList("avail"));
    private static final SinkContent defaultSinkContentOUpdate = new SinkContentBuilder().setName("SC Up").setResource("sink res").setDescription("desci").setSinkType(SinkContent.SinkType.OPENUPDATE).setSinkConfig(updateSC).build();
    private static final Sink defaultSink1 = new SinkBuilder().setId(111L).setVersion(222L).setContent(defaultSinkContent1).build();
    private static final Sink defaultSink2 = new SinkBuilder().setId(333L).setVersion(444L).setContent(defaultSinkContent2).build();
    private static final Sink defaultSinkOU = new SinkBuilder().setId(555L).setVersion(666L).setContent(defaultSinkContentOUpdate).build();
    private static final List<Sink> defaultSinkList = Arrays.asList(defaultSink1, defaultSink2);

    // Default SinkModels
    private static final SinkModel defaultSinkModel1 = new SinkModelBuilder()
            .setName("Sink Model Name 1")
            .setResource("Sink Model Resource 1")
            .setDescription("Sink Model Description 1")
            .build();
    private static final SinkModel defaultSinkModelOU = new SinkModelBuilder()
            .setSinkType(SinkContent.SinkType.OPENUPDATE)
            .setName("Name OU")
            .setResource("Resource OU")
            .setDescription("Description OU")
            .setSinkConfig(updateSC)
            .build();


    @Test(expected = NullPointerException.class)
    public void toModel_nullInput_throws() {
        // Activate Subject Under Test
        SinkModelMapper.toModel(null);
    }

    @Test
    public void toModel_validInput_returnsValidModel() {
        // Activate Subject Under Test
        SinkModel model = SinkModelMapper.toModel(defaultSink1);

        // Verification
        assertThat(model.getId(), is(defaultSink1.getId()));
        assertThat(model.getVersion(), is(defaultSink1.getVersion()));
        assertThat(model.getSinkName(), is(defaultSink1.getContent().getName()));
        assertThat(model.getResourceName(), is(defaultSink1.getContent().getResource()));
        assertThat(model.getDescription(), is(defaultSink1.getContent().getDescription()));
    }

    @Test
    public void toModel_validOUInput_returnsValidOUModel() {
        // Activate Subject Under Test
        SinkModel model = SinkModelMapper.toModel(defaultSinkOU);

        // Verification
        assertThat(model.getId(), is(555L));
        assertThat(model.getVersion(), is(666L));
        assertThat(model.getSinkName(), is("SC Up"));
        assertThat(model.getResourceName(), is("sink res"));
        assertThat(model.getDescription(), is("desci"));
        assertThat(model.getSinkType(), is(SinkContent.SinkType.OPENUPDATE));
        assertThat(model.getOpenUpdateUserId(), is("uid"));
        assertThat(model.getOpenUpdatePassword(), is("pwd"));
        assertThat(model.getOpenUpdateEndpoint(), is("url"));
    }

    @Test(expected = NullPointerException.class)
    public void toSinkContent_nullInput_throws() {
        // Activate Subject Under Test
        SinkModelMapper.toSinkContent(null);
    }

    @Test
    public void toSinkContent_validInput_returnsValidSinkContent() {
        // Activate Subject Under Test
        SinkContent sinkContent = SinkModelMapper.toSinkContent(defaultSinkModel1);

        // Verification
        assertThat(sinkContent.getName(), is(defaultSinkModel1.getSinkName()));
        assertThat(sinkContent.getResource(), is(defaultSinkModel1.getResourceName()));
        assertThat(sinkContent.getDescription(), is(defaultSinkModel1.getDescription()));
        assertThat(sinkContent.getSinkType(), is(SinkContent.SinkType.ES));
    }

    @Test
    public void toSinkContent_validOpenUpdateInput_returnsValidSinkContent() {
        // Activate Subject Under Test
        SinkContent sinkContent = SinkModelMapper.toSinkContent(defaultSinkModelOU);

        // Verification
        assertThat(sinkContent.getName(), is("Name OU"));
        assertThat(sinkContent.getResource(), is("Resource OU"));
        assertThat(sinkContent.getDescription(), is("Description OU"));
        assertThat(sinkContent.getSinkType(), is(SinkContent.SinkType.OPENUPDATE));
        OpenUpdateSinkConfig sinkConfig = (OpenUpdateSinkConfig) sinkContent.getSinkConfig();
        assertThat(sinkConfig.getUserId(), is(((OpenUpdateSinkConfig)updateSC).getUserId()));
        assertThat(sinkConfig.getPassword(), is(((OpenUpdateSinkConfig)updateSC).getPassword()));
        assertThat(sinkConfig.getEndpoint(), is(((OpenUpdateSinkConfig)updateSC).getEndpoint()));
        assertThat(sinkConfig.getAvailableQueueProviders(), is(((OpenUpdateSinkConfig) updateSC).getAvailableQueueProviders()));
    }

    @Test(expected = NullPointerException.class)
    public void toListOfSinkModels_nullInput_throws() {
        // Activate Subject Under Test
        SinkModelMapper.toListOfSinkModels(null);
    }

    @Test
    public void toListOfSinkModels_emptyInputList_returnsEmptyListOfSinkModels() {
        // Activate Subject Under Test
        List<SinkModel> sinkModels = SinkModelMapper.toListOfSinkModels(new ArrayList<Sink>());

        // Verification
        assertThat(sinkModels.size(), is(0));
    }

    @Test
    public void toListOfSinkModels_validListOfSinks_returnsValidListOfSinkModels() {
        // Activate Subject Under Test
        List<SinkModel> sinkModels = SinkModelMapper.toListOfSinkModels(defaultSinkList);

        // Verification
        assertThat(sinkModels.size(), is(2));
        assertThat(sinkModels.get(0).getId(), is(111L));
        assertThat(sinkModels.get(1).getId(), is(333L));
    }

    @Test
    public void toSinkContent_invalidSinkName_throwsIllegalArgumentException() {
        final String sinkName = "*%(Illegal)_&Name - €";
        final String expectedIllegalCharacters = "[*], [%], [(], [)], [&], [€]";
        SinkModel model = new SinkModelBuilder().setName(sinkName).build();
        try {
            SinkModelMapper.toSinkContent(model);
            fail("Illegal sink name not detected");
        } catch(IllegalArgumentException e) {
            assertThat(e.getMessage().contains(expectedIllegalCharacters), is (true));
        }
    }

}
