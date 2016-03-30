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

package dk.dbc.dataio.commons.utils.test;

import org.junit.Test;

import static dk.dbc.dataio.commons.utils.test.Assert.assertThat;
import static dk.dbc.dataio.commons.utils.test.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AssertTest {
    private final NullPointerException expectedException = new NullPointerException("test");
    private final String reason = "reason";
    private final Assert.ThrowingCodeBlock block = () -> {
        throw expectedException;
    };

    @Test
    public void assertThat_blockDoesNotThrow_throwsAssertionErrorWithReason() {
        try {
            assertThat(reason, () -> {}, isThrowing(NullPointerException.class));
            fail("No AssertionError thrown");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("reason : expected java.lang.NullPointerException to be thrown"));
        }
    }

    @Test
    public void assertThat_blockThrows_noAssertionErrorWithReasonThrown() {
        final NullPointerException exception = assertThat(reason, block, isThrowing(NullPointerException.class));
        assertThat(exception, is(expectedException));
    }

    @Test
    public void assertThat_blockThrowsUnexpectedException_throwsAssertionErrorWithReason() {
        try {
            assertThat(reason, block, isThrowing(IllegalArgumentException.class));
            fail("No AssertionError thrown");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("reason : expected java.lang.IllegalArgumentException to be thrown, was java.lang.NullPointerException"));
        }
    }

    @Test
    public void assertThat_blockDoesNotThrow_throwsAssertionError() {
        try {
            assertThat(() -> {}, isThrowing(NullPointerException.class));
            fail("No AssertionError thrown");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("expected java.lang.NullPointerException to be thrown"));
        }
    }

    @Test
    public void assertThat_blockThrows_noAssertionErrorThrown() {
        final NullPointerException exception = assertThat(block, isThrowing(NullPointerException.class));
        assertThat(exception, is(expectedException));
    }

    @Test
    public void assertThat_blockThrowsUnexpectedException_throwsAssertionError() {
        try {
            assertThat(block, isThrowing(IllegalArgumentException.class));
            fail("No AssertionError thrown");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("expected java.lang.IllegalArgumentException to be thrown, was java.lang.NullPointerException"));
        }
    }
}