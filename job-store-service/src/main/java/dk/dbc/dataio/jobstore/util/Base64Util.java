package dk.dbc.dataio.jobstore.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Base64Util {

    private static final Logger log = LoggerFactory.getLogger(Base64Util.class);
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    public static String base64encode(String dataToEncode) {
        return Base64.encodeBase64String(dataToEncode.getBytes(CHARSET));
    }

    public static String base64decode(String dataToDecode) {
        return new String(Base64.decodeBase64(dataToDecode), CHARSET);
    }

}
