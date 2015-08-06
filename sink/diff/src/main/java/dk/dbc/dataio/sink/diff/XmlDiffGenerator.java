package dk.dbc.dataio.sink.diff;

import dk.dbc.xmldiff.XmlDiff;
import dk.dbc.xmldiff.XmlDiffTextWriter;
import dk.dbc.xmldiff.XmlDiffWriter;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;


public class XmlDiffGenerator {
    private static final String EMPTY = "";

    // Tags marking difference in current
    private static final String OPEN_CURRENT = "[CURRENT>";
    private static final String CLOSE_CURRENT = "<CURRENT]";

    // Tags marking differences in next
    private static final String OPEN_LEFT = "[NEXT>";
    private static final String CLOSE_LEFT = "<NEXT]";

    // Name changed => namespace url is unchanged
    private static final String OPEN_NAME = "[NAME CHANGED>";
    private static final String CLOSE_NAME = "<NAME CHANGED]";

    // Namespace url changed (URI) => name is unchanged
    private static final String OPEN_URI = "[URI CHANGED>";
    private static final String CLOSE_URI = "<URI CHANGED]";

    public String getDiff(byte[] current, byte[] next) throws DiffGeneratorException {
        final XmlDiffWriter writer = new XmlDiffTextWriter(OPEN_CURRENT, CLOSE_CURRENT, OPEN_LEFT, CLOSE_LEFT, OPEN_NAME, CLOSE_NAME, OPEN_URI, CLOSE_URI);
        try {
            XmlDiff.Result result = XmlDiff.compare(
                    new ByteArrayInputStream(current),
                    new ByteArrayInputStream(next),
                    writer);
            if (hasDiff(result)) {
                return writer.toString();
            } else {
                return EMPTY;
            }
        } catch (SAXException | IOException e) {
           throw new DiffGeneratorException("XmlDiff Failed to compare input", e);

        }
    }

    boolean hasDiff(XmlDiff.Result result) throws IOException, SAXException {
        if (result.equals(XmlDiff.Result.DIFFERENT)) {
            return true;
        } else {
            return false;
        }
    }


}
