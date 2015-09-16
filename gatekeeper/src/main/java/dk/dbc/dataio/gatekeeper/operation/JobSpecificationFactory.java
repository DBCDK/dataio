package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;

import java.net.URISyntaxException;

/**
 * Factory class for the creation of job specifications from trans file entries
 */
public class JobSpecificationFactory {
    private JobSpecificationFactory() {}

    /**
     * Creates job specification from given transfile line using
     * "missing value" placeholders for missing field values
     * @param line transfile line to convert into job specification
     * @param transfileName name of parent transfile
     * @param fileStoreId file-store service ID of data file referenced in transfile line
     * @return JobSpecification instance
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued fileStoreId argument
     */
    public static JobSpecification createJobSpecification(TransFile.Line line, String transfileName, String fileStoreId)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(line, "line");
        InvariantUtil.checkNotNullNotEmptyOrThrow(transfileName, "transfileName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileStoreId, "fileStoreId");
        return new JobSpecification(
                getFieldValueOrMissing(line, "t"),
                getFieldValueOrMissing(line, "o"),
                getFieldValueOrMissing(line, "c"),
                getFieldValueOrMissing(line, "b"),
                getSubmitterIdOrMissing(line),
                getFieldValueOrMissing(line, "m"),
                getFieldValueOrMissing(line, "M"),
                getFieldValueOrMissing(line, "i"),
                getFileStoreUrnOrMissing(line, fileStoreId),
                JobSpecification.Type.PERSISTENT,
                getAncestry(transfileName, line));
    }

    private static String getFieldValueOrMissing(TransFile.Line line, String fieldName) {
        final String fieldValue = line.getField(fieldName);
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return Constants.MISSING_FIELD_VALUE;
        }
        return fieldValue;
    }

    private static long getSubmitterIdOrMissing(TransFile.Line line) {
        final String fieldValue = getFieldValueOrMissing(line, "f");
        if (Constants.MISSING_FIELD_VALUE.equals(fieldValue)) {
            return Constants.MISSING_SUBMITTER_VALUE;
        }
        try {
            final String submitter = fieldValue.substring(0, 6);
            return Long.valueOf(submitter);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return Constants.MISSING_SUBMITTER_VALUE;
        }
    }

    private static String getFileStoreUrnOrMissing(TransFile.Line line, String fileStoreId)
            throws IllegalArgumentException {
        final String fieldValue = getFieldValueOrMissing(line, "f");
        if (Constants.MISSING_FIELD_VALUE.equals(fieldValue)) {
            return Constants.MISSING_FIELD_VALUE;
        }
        if (Constants.MISSING_FIELD_VALUE.equals(fileStoreId)) {
            return fieldValue;
        }
        try {
            return FileStoreUrn.create(fileStoreId).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to create FileStoreUrn", e);
        }
    }

    private static JobSpecification.Ancestry getAncestry(String transfileName, TransFile.Line line) {
        final String datafileName = getFieldValueOrMissing(line, "f");
        final String batchId = getBatchId(datafileName);
        return new JobSpecification.Ancestry(transfileName, datafileName, batchId);
    }

    private static String getBatchId(String datafileName) {
        final String[] split = datafileName.split("\\.");
        if (split.length > 2) {
            return split[1];
        }
        return "";
    }
}
