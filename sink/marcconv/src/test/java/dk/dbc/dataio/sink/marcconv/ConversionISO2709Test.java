/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.sink.marcconv;

import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConversionISO2709Test {
    private final ConversionFactory conversionFactory = new ConversionFactory();

    @Test
    public void convert() {
        final byte[] in = ResourceReader.getResourceAsByteArray(ConversionISO2709Test.class,
                "test-record-danmarc2.marcXChange");
        final byte[] expected = ResourceReader.getResourceAsByteArray(ConversionISO2709Test.class,
                "test-record-danmarc2.iso");
        final Conversion conversion = conversionFactory.newConversion(
                new ConversionParam().withEncoding("danmarc2"));
        assertThat(conversion.apply(in), is(expected));
    }
}