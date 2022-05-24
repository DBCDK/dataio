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

package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * SinkContent unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SinkContentTest {
    private static final String NAME = "name";
    private static final String RESOURCE = "resource";
    private static final String DESCRIPTION = "description";
    private static final SinkContent.SinkType SINK_TYPE = SinkContent.SinkType.OPENUPDATE;
    private static final SinkConfig SINK_CONFIG = OpenUpdateSinkConfigTest.newOpenUpdateSinkConfigInstance();
    private static final SinkContent.SequenceAnalysisOption SEQUENCE_ANALYSIS_OPTION = SinkContent.SequenceAnalysisOption.ALL;

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new SinkContent(null, RESOURCE, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new SinkContent("", RESOURCE, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_resourceArgIsNull_throws() {
        new SinkContent(NAME, null, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_resourceArgIsEmpty_throws() {
        new SinkContent(NAME, "", DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_descriptionArgIsEmpty_returnsNewInstance() {
        new SinkContent(NAME, RESOURCE, "", SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_descriptionArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, RESOURCE, null, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_sinkTypeArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, RESOURCE, DESCRIPTION, null, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_sinkConfigArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, RESOURCE, DESCRIPTION, SINK_TYPE, null, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_sequenceAnalysisOptionArgIsNull_throws() {
        new SinkContent(NAME, RESOURCE, DESCRIPTION, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final SinkContent instance = new SinkContent(NAME, RESOURCE, DESCRIPTION, SINK_TYPE, SINK_CONFIG, SEQUENCE_ANALYSIS_OPTION);
        assertThat(instance, is(notNullValue()));
    }

    public static SinkContent newSinkContentInstance() {
        return new SinkContent(NAME, RESOURCE, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    public static SinkContent newSinkContentWithTypeInstance() {
        return new SinkContent(NAME, RESOURCE, DESCRIPTION, SINK_TYPE, SEQUENCE_ANALYSIS_OPTION);
    }

    public static SinkContent newSinkContentWithTypeAndConfigInstance() {
        return new SinkContent(NAME, RESOURCE, DESCRIPTION, SINK_TYPE, SINK_CONFIG, SEQUENCE_ANALYSIS_OPTION);
    }
}
