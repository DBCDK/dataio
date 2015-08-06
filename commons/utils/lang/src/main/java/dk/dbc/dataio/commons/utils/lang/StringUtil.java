package dk.dbc.dataio.commons.utils.lang;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringUtil {
    public static final Charset STANDARD_CHARSET = StandardCharsets.UTF_8;

    private StringUtil() {}

    public static byte[] asBytes(String str) {
        return asBytes(str, STANDARD_CHARSET);
    }

    public static byte[] asBytes(String str, Charset encoding) {
        if (str != null)
            return str.getBytes(encoding);
        return new byte[0];
    }

    public static String asString(byte[] bytes) {
        return asString(bytes, STANDARD_CHARSET);
    }

    public static String asString(byte[] bytes, Charset encoding) {
        if (bytes != null)
            return new String(bytes, encoding);
        return "";
    }

    public static String base64encode(String str) {
        return base64encode(str, STANDARD_CHARSET);
    }

    public static String base64encode(String str, Charset encoding) {
        return Base64.encodeBase64String(str.getBytes(encoding));
    }

    public static String base64decode(String str) {
        return base64decode(str, STANDARD_CHARSET);
    }

    public static String base64decode(String str, Charset encoding) {
        return new String(Base64.decodeBase64(str), encoding);
    }
}
