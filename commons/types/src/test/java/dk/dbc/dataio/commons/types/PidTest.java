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

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class PidTest {
    @Test
    public void pidHasNoColon_throws() {
        assertThat(() -> Pid.of("nocolon"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void pidMissingType_throws() {
        assertThat(() -> Pid.of(":id"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void pidMissingBibliographicId_throws() {
        assertThat(() -> Pid.of("unit:"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void pidMissingFormat_throws() {
        assertThat(() -> Pid.of("870970:23142546"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void pidFormatIsEmpty_throws() {
        assertThat(() -> Pid.of("870970-:23142546"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void pidAgencyIdHasLessThatSixDigits_throws() {
        assertThat(() -> Pid.of("99999-basis:23142546"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void pidAgencyIdHasMoreThatSixDigits_throws() {
        assertThat(() -> Pid.of("1000000-basis:23142546"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void pidOfTypeUnit() {
        final String value = "unit:1354373";
        final Pid pid = Pid.of(value);
        assertThat("type", pid.getType(), is(Pid.Type.UNIT));
        assertThat("value", pid.toString(), is(value));
        assertThat("agencyId", pid.getAgencyId(), is(nullValue()));
        assertThat("bibliographicRecordId", pid.getBibliographicRecordId(), is("1354373"));
        assertThat("format", pid.getFormat(), is(nullValue()));
    }

    @Test
    public void pidOfTypeWork() {
        final String value = "work:995029";
        final Pid pid = Pid.of(value);
        assertThat("type", pid.getType(), is(Pid.Type.WORK));
        assertThat("value", pid.toString(), is(value));
        assertThat("agencyId", pid.getAgencyId(), is(nullValue()));
        assertThat("bibliographicRecordId", pid.getBibliographicRecordId(), is("995029"));
        assertThat("format", pid.getFormat(), is(nullValue()));
    }

    @Test
    public void pidOfTypeBibliographicObject() {
        final String value = "870970-basis:23142546";
        final Pid pid = Pid.of(value);
        assertThat("type", pid.getType(), is(Pid.Type.BIBLIOGRAPHIC_OBJECT));
        assertThat("value", pid.toString(), is(value));
        assertThat("agencyId", pid.getAgencyId(), is(870970));
        assertThat("bibliographicRecordId", pid.getBibliographicRecordId(), is("23142546"));
        assertThat("format", pid.getFormat(), is("basis"));
    }

    @Test
    public void pidIsMultipart() {
        final String value = "870970-basis:23142546_1";
        final Pid pid = Pid.of(value);
        assertThat("type", pid.getType(), is(Pid.Type.BIBLIOGRAPHIC_OBJECT));
        assertThat("value", pid.toString(), is(value));
        assertThat("agencyId", pid.getAgencyId(), is(870970));
        assertThat("bibliographicRecordId", pid.getBibliographicRecordId(), is("23142546"));
        assertThat("format", pid.getFormat(), is("basis"));
    }
}