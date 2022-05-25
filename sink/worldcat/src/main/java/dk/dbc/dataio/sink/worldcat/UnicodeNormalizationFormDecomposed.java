package dk.dbc.dataio.sink.worldcat;

import java.text.Normalizer;

public class UnicodeNormalizationFormDecomposed {
    private UnicodeNormalizationFormDecomposed() {}

    public static String of(String original) {
        return Normalizer.normalize(original, Normalizer.Form.NFD);
    }
}
