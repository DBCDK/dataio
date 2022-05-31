package dk.dbc.dataio.commons.conversion;

public class Conversion {
    final ConversionParam param;

    public Conversion(ConversionParam param) {
        this.param = param;
    }

    public ConversionParam getParam() {
        return param;
    }

    public byte[] apply(byte[] bytes) {
        throw new UnsupportedOperationException("apply not implemented for conversion");
    }
}
