package dk.dbc.dataio.commons.conversion;

import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
