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
import static org.junit.Assert.assertThat;

/**
 * SubmitterContent unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SubmitterContentTest {
    private static final long NUMBER = 42L;
    private static final String NAME = "NAME";
    private static final String DESCRIPTION = "description";

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new SubmitterContent(NUMBER, null, DESCRIPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new SubmitterContent(NUMBER, "", DESCRIPTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_descriptionArgIsNull_throws() {
        new SubmitterContent(NUMBER, NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_descriptionArgIsEmpty_throws() {
        new SubmitterContent(NUMBER, NAME, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_numberArgIsLessThanLowerBound_throws() {
        new SubmitterContent(Constants.PERSISTENCE_ID_LOWER_BOUND - 1, NAME, DESCRIPTION);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final SubmitterContent instance = new SubmitterContent(NUMBER, NAME, DESCRIPTION);
        assertThat(instance, is(notNullValue()));
    }

    public static SubmitterContent newSubmitterContentInstance() {
        return new SubmitterContent(NUMBER, NAME, DESCRIPTION);
    }
}
