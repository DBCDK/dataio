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
 * JavaScript unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class JavaScriptTest {
    private static final String MODULE_NAME = "module";
    private static final String JAVASCRIPT = "javascript";

    @Test(expected = NullPointerException.class)
    public void constructor_javascriptArgIsNull_throws() {
        new JavaScript(null, MODULE_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_javascriptArgIsEmpty_throws() {
        new JavaScript("", MODULE_NAME);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_moduleNameArgIsNull_throws() {
        new JavaScript(JAVASCRIPT, null);
    }

    @Test
    public void constructor_moduleNameArgIsEmpty_returnsNewInstance() {
        final JavaScript instance = new JavaScript(JAVASCRIPT, "");
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final JavaScript instance = new JavaScript(JAVASCRIPT, MODULE_NAME);
        assertThat(instance, is(notNullValue()));
    }

    public static JavaScript newJavaScriptInstance() {
        return new JavaScript(JAVASCRIPT, MODULE_NAME);
    }
}
