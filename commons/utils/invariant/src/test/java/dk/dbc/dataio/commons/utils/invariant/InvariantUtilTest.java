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

package dk.dbc.dataio.commons.utils.invariant;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * InvariantUtil unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class InvariantUtilTest {
    private final String parameterName = "name";
    private final long lower_bound = 0;

    @Test(expected = NullPointerException.class)
    public void checkNotNullOrThrow_objectArgIsNull_throws() {
        InvariantUtil.checkNotNullOrThrow(null, parameterName);
    }

    @Test
    public void checkNotNullOrThrow_objectArgIsNonNull_returnsObject() {
        final Object object = new Object();
        final Object returnedObject = InvariantUtil.checkNotNullOrThrow(object, parameterName);
        assertThat(returnedObject, is(object));
    }

    @Test(expected = NullPointerException.class)
    public void checkNotNullNotEmptyOrThrow_stringObjectArgIsNull_throws() {
        InvariantUtil.checkNotNullNotEmptyOrThrow(null, parameterName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkNotNullNotEmptyOrThrow_stringObjectArgIsEmpty_throws() {
        InvariantUtil.checkNotNullNotEmptyOrThrow("", parameterName);
    }

    @Test
    public void checkNotNullNotEmptyOrThrow_stringObjectArgIsNonEmpty_returnsObject() {
        final String object = "string";
        final String returnedObject = InvariantUtil.checkNotNullOrThrow(object, parameterName);
        assertThat(returnedObject, is(object));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkLowerBoundOrThrow_valueArgIsLessThan_throws() {
        InvariantUtil.checkLowerBoundOrThrow(-42, parameterName, lower_bound);
    }

    @Test
    public void checkLowerBoundOrThrow_valueArgEqualsBound_returnsValue() {
        assertThat(InvariantUtil.checkLowerBoundOrThrow(lower_bound, parameterName, lower_bound), is(lower_bound));
    }

    @Test
    public void checkLowerBoundOrThrow_valueArgIsAboveBound_returnsValue() {
        final long expectedValue = 42L;
        assertThat(InvariantUtil.checkLowerBoundOrThrow(expectedValue, parameterName, lower_bound), is(expectedValue));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkIntLowerBoundOrThrow_valueArgIsLessThan_throws() {
        InvariantUtil.checkIntLowerBoundOrThrow(-42, parameterName, 0);
    }

    @Test
    public void checkIntLowerBoundOrThrow_valueArgEqualsBound_returnsValue() {
        final int expectedValue = 0;
        assertThat(InvariantUtil.checkIntLowerBoundOrThrow(expectedValue, parameterName, expectedValue), is(expectedValue));
    }

    @Test
    public void checkIntLowerBoundOrThrow_valueArgIsAboveBound_returnsValue() {
        final int expectedValue = 42;
        assertThat(InvariantUtil.checkIntLowerBoundOrThrow(expectedValue, parameterName, 0), is(expectedValue));
    }
}
