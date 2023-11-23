package dk.dbc.dataio.commons.conversion;

import org.junit.jupiter.api.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

public class ConversionFactoryTest {
    @Test
    public void conversionParamWithoutPackaging() {
        ConversionParam param = new ConversionParam();
        assertThat(new ConversionFactory().newConversion(param), is(instanceOf(ConversionNOOP.class)));
    }

    @Test
    public void conversionParamWithEmptyPackaging() {
        ConversionParam param = new ConversionParam().withPackaging("  ");
        assertThat(new ConversionFactory().newConversion(param), is(instanceOf(ConversionNOOP.class)));
    }

    @Test
    public void conversionParamWithIsoPackaging() {
        ConversionParam param = new ConversionParam().withPackaging("Iso");
        assertThat(new ConversionFactory().newConversion(param), is(instanceOf(ConversionISO2709.class)));
    }

    @Test
    public void conversionParamWithUnknownPackaging() {
        ConversionParam param = new ConversionParam().withPackaging("unknown");
        assertThat(() -> new ConversionFactory().newConversion(param), isThrowing(ConversionException.class));
    }
}
