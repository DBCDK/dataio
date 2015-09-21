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

package dk.dbc.dataio.jobstore.service.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EncodingsUtilTest {
    @Test
    public void isEquivalent_firstArgIsNull_returnsFalse() {
        assertThat(EncodingsUtil.isEquivalent(null, "utf8"), is(false));
    }

    @Test
    public void isEquivalent_secondArgIsNull_returnsFalse() {
        assertThat(EncodingsUtil.isEquivalent("utf8", null), is(false));
    }

    @Test
    public void isEquivalent_bothArgsAreNull_returnsFalse() {
        assertThat(EncodingsUtil.isEquivalent(null, null), is(false));
    }

    @Test
    public void isEquivalent_argsAreEquivalent_returnsTrue() {
        assertThat(EncodingsUtil.isEquivalent("utf-8", "UTF8"), is(true));
    }

    @Test
    public void isEquivalent_argsAreNotEquivalent_returnsFalse() {
        assertThat(EncodingsUtil.isEquivalent("utf8", "latin1"), is(false));
    }
}