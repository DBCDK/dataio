package dk.dbc.dataio.commons.types;

import java.util.Arrays;
import java.util.List;


public abstract class RecordSplitterConstants {
    public enum RecordSplitter {
        ADDI,
        ADDI_MARC_XML,
        CSV,
        DANMARC2_LINE_FORMAT,
        DANMARC2_LINE_FORMAT_COLLECTION,
        DSD_CSV,
        ISO2709,
        ISO2709_COLLECTION,
        JSON,
        VIAF,
        VIP_CSV,
        XML,
        TARRED_XML,
        ZIPPED_XML
    }

    /**
     * @return the list of recordSplitters, containing all available recordSplitters
     */
    public static List<RecordSplitter> getRecordSplitters() {
        return Arrays.asList(RecordSplitter.values());
    }
}
