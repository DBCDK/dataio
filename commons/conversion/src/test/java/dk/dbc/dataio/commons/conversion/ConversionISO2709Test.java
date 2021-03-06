/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.commons.conversion;

import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConversionISO2709Test {
    private final ConversionFactory conversionFactory = new ConversionFactory();

    @Test
    public void convert() {
        final byte[] in = ResourceReader.getResourceAsByteArray(ConversionISO2709Test.class,
                "test-record-1-danmarc2.marcxchange");
        final byte[] expected = ResourceReader.getResourceAsByteArray(ConversionISO2709Test.class,
                "test-record-1-danmarc2.iso");
        final Conversion conversion = conversionFactory.newConversion(
                new ConversionParam().withPackaging("iso").withEncoding("danmarc2"));
        assertThat(conversion.apply(in), is(expected));
    }
}