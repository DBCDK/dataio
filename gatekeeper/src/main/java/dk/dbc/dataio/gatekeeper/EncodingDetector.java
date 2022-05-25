package dk.dbc.dataio.gatekeeper;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Detects encoding of character data
 */
public class EncodingDetector {
    public static String BOM = "\uFEFF";

    private final CharsetDetector charsetDetector;

    public EncodingDetector() {
        charsetDetector = new CharsetDetector();
    }

    /**
     * Detects the charset that best matches the supplied text file
     * @param file path to text file of unknown encoding
     * @return charset or empty
     */
    public Optional<Charset> detect(Path file) {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file))) {
            charsetDetector.setText(inputStream);
            final CharsetMatch match = charsetDetector.detect();
            if (match != null) {
                return Optional.of(Charset.forName(match.getName()));
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Detects the charset that best matches the supplied input data
     * @param bytes the input text of unknown encoding
     * @return charset or empty
     */
    public Optional<Charset> detect(byte[] bytes) {
        charsetDetector.setText(bytes);
        final CharsetMatch match = charsetDetector.detect();
        if (match != null) {
            return Optional.of(Charset.forName(match.getName()));
        }
        return Optional.empty();
    }
}
