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

package dk.dbc.dataio.commons.utils.service;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class HowRUTest {
    @Test
    public void defaultOk() {
        assertThat(new HowRU().toJson(), is("{\"ok\":true}"));
    }

    @Test
    public void withException() {
        try {
            throw new NullPointerException("death by NPE");
        } catch (NullPointerException e) {
            final HowRU howRU = new HowRU().withException(e);
            assertThat("isOK()", howRU.isOk(), is(false));
            assertThat("getErrorText()", howRU.getErrorText(), is("death by NPE"));
            assertThat("getError()", howRU.getError(), is(notNullValue()));
            assertThat("getError().getMessage()", howRU.getError().getMessage(), is("death by NPE"));
            assertThat("getError().getStacktrace()", howRU.getError().getStacktrace(), is(notNullValue()));
        }
    }
}
