package dk.dbc.dataio.commons.types;

import java.util.Arrays;
import java.util.List;


public abstract class RecordSplitterConstants {

    public enum RecordSplitter { XML, ISO2709 }

    /**
     * @return the list of recordSplitters, containing all available recordSplitters
     */
    public static List<RecordSplitter> getRecordSplitters() {
        return Arrays.asList(RecordSplitter.values());
    }
}
