package dk.dbc.dataio.commons;

import dk.dbc.dataio.commons.partioner.AddiDataPartitioner;
import dk.dbc.dataio.commons.partioner.CsvDataPartitioner;
import dk.dbc.dataio.commons.partioner.DanMarc2LineFormatDataPartitioner;
import dk.dbc.dataio.commons.partioner.DanMarc2LineFormatReorderingDataPartitioner;
import dk.dbc.dataio.commons.partioner.DataPartitioner;
import dk.dbc.dataio.commons.partioner.DefaultXmlDataPartitioner;
import dk.dbc.dataio.commons.partioner.DsdCsvDataPartitioner;
import dk.dbc.dataio.commons.partioner.Iso2709DataPartitioner;
import dk.dbc.dataio.commons.partioner.Iso2709ReorderingDataPartitioner;
import dk.dbc.dataio.commons.partioner.JsonDataPartitioner;
import dk.dbc.dataio.commons.partioner.MarcXchangeAddiDataPartitioner;
import dk.dbc.dataio.commons.partioner.TarredXmlDataPartitioner;
import dk.dbc.dataio.commons.partioner.ViafDataPartitioner;
import dk.dbc.dataio.commons.partioner.VipCsvDataPartitioner;
import dk.dbc.dataio.commons.partioner.VolumeAfterParents;
import dk.dbc.dataio.commons.partioner.VolumeIncludeParents;
import dk.dbc.dataio.commons.partioner.ZippedXmlDataPartitioner;
import dk.dbc.dataio.commons.types.JobSpecification;
import jakarta.persistence.EntityManager;

import java.io.InputStream;
import java.util.function.BiFunction;

public enum RecordSplitter {
    ADDI(AddiDataPartitioner::newInstance),
    ADDI_MARC_XML(MarcXchangeAddiDataPartitioner::newInstance),
    CSV(CsvDataPartitioner::newInstance),
    DANMARC2_LINE_FORMAT(RecordSplitter::getDanMarc2LineFormatPartitioner),
    DANMARC2_LINE_FORMAT_COLLECTION((is, js, id, em) -> DanMarc2LineFormatReorderingDataPartitioner.newInstance(is, js.getCharset(), new VolumeIncludeParents(id, em))),
    DSD_CSV(DsdCsvDataPartitioner::newInstance),
    ISO2709(RecordSplitter::getIso2709Partitioner),
    ISO2709_COLLECTION((is, js, id, em) -> Iso2709ReorderingDataPartitioner.newInstance(is, js.getCharset(), new VolumeIncludeParents(id, em))),
    JSON(JsonDataPartitioner::newInstance),
    VIAF(ViafDataPartitioner::newInstance),
    VIP_CSV(VipCsvDataPartitioner::newInstance),
    XML(DefaultXmlDataPartitioner::newInstance),
    TARRED_XML(TarredXmlDataPartitioner::newInstance),
    ZIPPED_XML(ZippedXmlDataPartitioner::newInstance);

    private final PartitionerInput pi;

    RecordSplitter(PartitionerInput partitionerInput) {
        pi = partitionerInput;
    }

    RecordSplitter(BiFunction<InputStream, String, DataPartitioner> f) {
        pi = (is, js, id, em) -> f.apply(is, js.getCharset());
    }

    public DataPartitioner toPartitioner(InputStream dataFileInputStream, JobSpecification jobSpecification, int jobId, EntityManager em) {
        return pi.from(dataFileInputStream, jobSpecification, jobId, em);
    }

    public interface PartitionerInput {
        DataPartitioner from(InputStream dataFileInputStream, JobSpecification jobSpecification, int jobId, EntityManager em);
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
