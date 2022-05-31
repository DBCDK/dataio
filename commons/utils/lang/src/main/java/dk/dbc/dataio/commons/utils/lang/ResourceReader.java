package dk.dbc.dataio.commons.utils.lang;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/*
 * This class handles reading of resources.
 */
public class ResourceReader {
    public static InputStream getResourceAsStream(Class aClass, String resourceName) {
        return aClass.getClassLoader().getResourceAsStream(resourceName);
    }

    public static byte[] getResourceAsByteArray(Class aClass, String resourceName) {
        final byte[] byteBuffer = new byte[8192];
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = aClass.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException("The resource '" + resourceName + "' could not be found on the classpath");
            }
            int bytesRead = in.read(byteBuffer);
            while (bytesRead != -1) {
                out.write(byteBuffer, 0, bytesRead);
                bytesRead = in.read(byteBuffer);
            }
        } catch (IOException e) {
            throw new IllegalStateException("The resource '" + resourceName + "' could not be read", e);
        }
        return out.toByteArray();
    }

    public static String getResourceAsString(Class aClass, String resourceName) {
        return StringUtil.asString(getResourceAsByteArray(aClass, resourceName));
    }

    public static String getResourceAsBase64(Class aClass, String resourceName) {
        return Base64.encodeBase64String(getResourceAsByteArray(aClass, resourceName));
    }
}
