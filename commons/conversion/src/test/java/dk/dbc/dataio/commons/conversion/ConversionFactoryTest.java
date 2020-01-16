/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.commons.conversion;

import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

public class ConversionFactoryTest {
    @Test
    public void conversionParamWithoutPackaging() {
        final ConversionParam param = new ConversionParam();
        assertThat(new ConversionFactory().newConversion(param), is(instanceOf(ConversionNOOP.class)));
    }

    @Test
    public void conversionParamWithEmptyPackaging() {
        final ConversionParam param = new ConversionParam().withPackaging("  ");
        assertThat(new ConversionFactory().newConversion(param), is(instanceOf(ConversionNOOP.class)));
    }

    @Test
    public void conversionParamWithIsoPackaging() {
        final ConversionParam param = new ConversionParam().withPackaging("Iso");
        assertThat(new ConversionFactory().newConversion(param), is(instanceOf(ConversionISO2709.class)));
    }

    @Test
    public void conversionParamWithUnknownPackaging() {
        final ConversionParam param = new ConversionParam().withPackaging("unknown");
        assertThat(() -> new ConversionFactory().newConversion(param), isThrowing(ConversionException.class));
    }
}