package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitter;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static dk.dbc.dataio.commons.types.RecordSplitter.ADDI;
import static dk.dbc.dataio.commons.types.RecordSplitter.ADDI_MARC_XML;
import static dk.dbc.dataio.commons.types.RecordSplitter.CSV;
import static dk.dbc.dataio.commons.types.RecordSplitter.DANMARC2_LINE_FORMAT;
import static dk.dbc.dataio.commons.types.RecordSplitter.DANMARC2_LINE_FORMAT_COLLECTION;
import static dk.dbc.dataio.commons.types.RecordSplitter.DSD_CSV;
import static dk.dbc.dataio.commons.types.RecordSplitter.ISO2709;
import static dk.dbc.dataio.commons.types.RecordSplitter.ISO2709_COLLECTION;
import static dk.dbc.dataio.commons.types.RecordSplitter.JSON;
import static dk.dbc.dataio.commons.types.RecordSplitter.TARRED_XML;
import static dk.dbc.dataio.commons.types.RecordSplitter.VIAF;
import static dk.dbc.dataio.commons.types.RecordSplitter.VIP_CSV;
import static dk.dbc.dataio.commons.types.RecordSplitter.XML;
import static dk.dbc.dataio.commons.types.RecordSplitter.ZIPPED_XML;
import static dk.dbc.dataio.commons.types.RecordSplitter.values;

/**
 * Factory interface for creation of instances of DataPartitioner
 */
public class DataPartitionerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPartitionerFactory.class);
    private static final Map<RecordSplitter, PartitionerInput> map = createMappings();

    private static Map<RecordSplitter, PartitionerInput> createMappings() {
        Map<RecordSplitter, PartitionerInput> map = Map.ofEntries(
                Map.entry(ADDI, toInput(AddiDataPartitioner::newInstance)),
                Map.entry(ADDI_MARC_XML, toInput(MarcXchangeAddiDataPartitioner::newInstance)),
                Map.entry(CSV, toInput(CsvDataPartitioner::newInstance)),
                Map.entry(DANMARC2_LINE_FORMAT, DataPartitionerFactory::getDanMarc2LineFormatPartitioner),
                Map.entry(DANMARC2_LINE_FORMAT_COLLECTION, (is, js, id, em) -> DanMarc2LineFormatReorderingDataPartitioner.newInstance(is, js.getCharset(), new VolumeIncludeParents(id, em))),
                Map.entry(DSD_CSV, toInput(DsdCsvDataPartitioner::newInstance)),
                Map.entry(ISO2709, DataPartitionerFactory::getIso2709Partitioner),
                Map.entry(ISO2709_COLLECTION, (is, js, id, em) -> Iso2709ReorderingDataPartitioner.newInstance(is, js.getCharset(), new VolumeIncludeParents(id, em))),
                Map.entry(JSON, toInput(JsonDataPartitioner::newInstance)),
                Map.entry(VIAF, toInput(ViafDataPartitioner::newInstance)),
                Map.entry(VIP_CSV, toInput(VipCsvDataPartitioner::newInstance)),
                Map.entry(XML, toInput(DefaultXmlDataPartitioner::newInstance)),
                Map.entry(TARRED_XML, toInput(TarredXmlDataPartitioner::newInstance)),
                Map.entry(ZIPPED_XML, toInput(ZippedXmlDataPartitioner::newInstance))
        );
        Set<RecordSplitter> unmatched = Arrays.stream(values()).filter(e -> !map.containsKey(e)).collect(Collectors.toSet());
        if(!unmatched.isEmpty()) LOGGER.error("All record splitters must mapped to a data partitioner, unmapped: " + unmatched);
        return map;
    }

    public static DataPartitioner create(RecordSplitter recordSplitter, InputStream inputStream, JobSpecification jobSpecification, int jobId, EntityManager em) {
        PartitionerInput partitionerInput = map.get(recordSplitter);
        return partitionerInput.from(inputStream, jobSpecification, jobId, em);
    }

    private static DataPartitioner getDanMarc2LineFormatPartitioner(InputStream is, JobSpecification js, int jobId, EntityManager em) {
        String encoding = js.getCharset();
        switch (TypeOfReordering.from(js)) {
            case VOLUME_INCLUDE_PARENTS:
                return DanMarc2LineFormatReorderingDataPartitioner.newInstance(is, encoding, new VolumeIncludeParents(jobId, em));
            case VOLUME_AFTER_PARENTS:
                return DanMarc2LineFormatReorderingDataPartitioner.newInstance(is, encoding, new VolumeAfterParents(jobId, em));
            default:
                return DanMarc2LineFormatDataPartitioner.newInstance(is, encoding);
        }
    }

    private static DataPartitioner getIso2709Partitioner(InputStream is, JobSpecification js, int jobId, EntityManager em) {
        String encoding = js.getCharset();
        switch (TypeOfReordering.from(js)) {
            case VOLUME_INCLUDE_PARENTS:
                return Iso2709ReorderingDataPartitioner.newInstance(is, encoding, new VolumeIncludeParents(jobId, em));
            case VOLUME_AFTER_PARENTS:
                return Iso2709ReorderingDataPartitioner.newInstance(is, encoding, new VolumeAfterParents(jobId, em));
            default:
                return Iso2709DataPartitioner.newInstance(is, encoding);
        }
    }

    static PartitionerInput toInput(BiFunction<InputStream, String, DataPartitioner> f) {
        return (is, js, id, em) -> f.apply(is, js.getCharset());
    }

    public interface PartitionerInput {
        DataPartitioner from(InputStream dataFileInputStream, JobSpecification jobSpecification, int jobId, EntityManager em);
    }

    private enum TypeOfReordering {
        VOLUME_AFTER_PARENTS,
        VOLUME_INCLUDE_PARENTS,
        NONE;

        public static TypeOfReordering from(JobSpecification jobSpecification) {
            JobSpecification.Ancestry ancestry = jobSpecification.getAncestry();
            // Items originating from external sources must undergo potential re-ordering
            if (ancestry != null && ancestry.getTransfile() != null && !jobSpecification.getType().canBePreview()) {
                return TypeOfReordering.VOLUME_AFTER_PARENTS;
            }
            return TypeOfReordering.NONE;
        }
    }
}
