package dk.dbc.dataio.commons.encoding;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.marc.Marc8Charset;

import java.nio.charset.Charset;

public class CharacterEncodingScheme {
    private CharacterEncodingScheme() {
    }

    /**
     * Resolves {@link Charset} from given character set name (or alias)
     *
     * @param name character set name (is automatically lowercased and trimmed)
     * @return {@link Charset} instance
     * @throws InvalidEncodingException if unable to resolve into {@link Charset}
     */
    public static Charset charsetOf(String name) throws InvalidEncodingException {
        try {
            final String normalizedEncodingName = normalizeEncodingName(name);
            if ("marc8".equals(normalizedEncodingName)) {
                return new Marc8Charset();
            }
            return Charset.forName(normalizedEncodingName);
        } catch (RuntimeException e) {
            throw new InvalidEncodingException(String.format("Unable to create charset from given name '%s'", name), e);
        }
    }

    private static String normalizeEncodingName(String name) {
        final String normalized = name.trim().toLowerCase();
        if ("latin-1".equals(normalized)) {
            return "latin1";
        }
        if ("marc-8".equals(normalized)) {
            return "marc8";
        }
        return normalized;
    }
}
