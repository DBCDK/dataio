package dk.dbc.dataio.commons.utils.lang;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringUtil {
    public static final Charset STANDARD_CHARSET = StandardCharsets.UTF_8;

    private StringUtil() {
    }

    public static byte[] asBytes(String str) {
        return asBytes(str, STANDARD_CHARSET);
    }

    public static byte[] asBytes(String str, Charset encoding) {
        if (str != null)
            return str.getBytes(encoding);
        return new byte[0];
    }

    public static InputStream asInputStream(String s) {
        return asInputStream(s, STANDARD_CHARSET);
    }

    public static InputStream asInputStream(String s, Charset encoding) {
        return new ByteArrayInputStream(s.getBytes(encoding));
    }

    public static String asString(byte[] bytes) {
        return asString(bytes, STANDARD_CHARSET);
    }

    public static String asString(byte[] bytes, Charset encoding) {
        if (bytes != null)
            return new String(bytes, encoding);
        return "";
    }

    public static String asString(InputStream is) {
        if (is != null) {
            return new String(asBytes(is), STANDARD_CHARSET);
        }
        return "";
    }

    public static String asString(InputStream is, Charset encoding) {
        if (is != null) {
            return new String(asBytes(is), encoding);
        }
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

    public static String getStackTraceString(Throwable t, String indent) {
        final StringBuilder sb = new StringBuilder();
        sb.append(t.toString());
        sb.append("\n");

        final StackTraceElement[] stack = t.getStackTrace();
        if (stack != null) {
            for (StackTraceElement stackTraceElement : stack) {
                sb.append(indent);
                sb.append("\tat ");
                sb.append(stackTraceElement.toString());
                sb.append("\n");
            }
        }

        final Throwable[] suppressedExceptions = t.getSuppressed();
        // Print suppressed exceptions indented one level deeper.
        if (suppressedExceptions != null) {
            for (Throwable throwable : suppressedExceptions) {
                sb.append(indent);
                sb.append("\tSuppressed: ");
                sb.append(getStackTraceString(throwable, indent + "\t"));
            }
        }

        final Throwable cause = t.getCause();
        if (cause != null) {
            sb.append(indent);
            sb.append("Caused by: ");
            sb.append(getStackTraceString(cause, indent));
        }

        return sb.toString();
    }

    public static String getStackTraceString(Throwable t) {
        String failureMsg;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
            t.printStackTrace(ps);
            failureMsg = baos.toString("UTF-8");
        } catch (IOException e) {
            failureMsg = e.getMessage();
        }
        return failureMsg;
    }

    public static String removeWhitespace(final String str) {
        final char[] chars = str.toCharArray();
        int pos = 0;
        for (int i = 0; i < chars.length; i++) {
            if (!Character.isWhitespace(chars[i])) {
                chars[pos++] = chars[i];
            }
        }
        return new String(chars, 0, pos);
    }

    private static byte[] asBytes(InputStream is) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8096];
            int length;
            while ((length = is.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
