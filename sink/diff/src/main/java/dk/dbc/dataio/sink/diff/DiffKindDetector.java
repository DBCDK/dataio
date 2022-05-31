package dk.dbc.dataio.sink.diff;

public class DiffKindDetector {
    private DiffKindDetector() {
    }

    public static ExternalToolDiffGenerator.Kind getKind(byte[] data) {
        if (data != null && data.length > 0) {
            if (data[0] == '<') {
                return ExternalToolDiffGenerator.Kind.XML;
            }
            if (data[0] == '{' || data[0] == '[') {
                return ExternalToolDiffGenerator.Kind.JSON;
            }
        }
        return ExternalToolDiffGenerator.Kind.PLAINTEXT;
    }
}
