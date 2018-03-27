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

package dk.dbc.httpclient;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PathBuilderTest {
    private static final String PATH_TEMPLATE = "{id1}/test/{id2}/{id1}/test/{id2}";

    @Test(expected = NullPointerException.class)
    public void constructor_pathTemplateArgIsNull_throws() {
        new PathBuilder(null);
    }

    @Test
    public void pathBuilder_noValuesBound_returnsPathTemplateUnchanged() {
        final PathBuilder pathBuilder = new PathBuilder(PATH_TEMPLATE);
        assertThat(pathBuilder.build(), is(PATH_TEMPLATE.split(PathBuilder.PATH_SEPARATOR)));
    }

    @Test
    public void pathBuilder_whenValuesMatchPathVariables_returnsInterpolatedPath() {
        final String expectedPath = "val1/test/val2/val1/test/val2";
        final PathBuilder pathBuilder = new PathBuilder(PATH_TEMPLATE);
        pathBuilder.bind("id1", "val1");
        pathBuilder.bind("id2", "val2");
        assertThat(pathBuilder.build(), is(expectedPath.split(PathBuilder.PATH_SEPARATOR)));
    }
}