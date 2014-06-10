package dk.dbc.dataio.commons.utils.service;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Base64Util {
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    public static String base64encode(String dataToEncode) {
        return Base64.encodeBase64String(dataToEncode.getBytes(CHARSET));
    }

    public static String base64decode(String dataToDecode) {
        return new String(Base64.decodeBase64(dataToDecode), CHARSET);
    }
}
