package dk.dbc.dataio.commons.conversion;

public class ConversionNOOP extends Conversion {
    public ConversionNOOP() {
        this(null);
    }

    public ConversionNOOP(ConversionParam param) {
        super(param);
    }

    @Override
    public byte[] apply(byte[] bytes) {
        return bytes;
    }
}
