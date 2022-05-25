package dk.dbc.dataio.addi.bindings;

public class SinkDirectives {
    public boolean encodeAs2709;
    public String charset;

    public SinkDirectives withEncodeAs2709(boolean encodeAs2709) {
        this.encodeAs2709 = encodeAs2709;
        return this;
    }

    public SinkDirectives withCharset(String charset) {
        this.charset = charset;
        return this;
    }
}
