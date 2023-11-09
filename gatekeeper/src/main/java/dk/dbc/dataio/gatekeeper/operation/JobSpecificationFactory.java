package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gatekeeper.Util;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for the creation of job specifications from trans file entries
 */
public class JobSpecificationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSpecificationFactory.class);
    public static final String DESTINATION_MARCKONV = "marckonv";
    public static final String DESTINATION_DANBIB = "danbib";
    public static final String PACKAGING_DANBIB_DEFAULT = "iso";
    public static final String ENCODING_DANBIB_DEFAULT = "latin-1";

    private JobSpecificationFactory() {
    }

    /**
     * Creates job specification from given transfile line using
     * "missing value" placeholders for missing field values. In case destination
     * is {@value #DESTINATION_DANBIB} missing values for packaging and/or
     * encoding fields will be set to {@value PACKAGING_DANBIB_DEFAULT} and
     * {@value ENCODING_DANBIB_DEFAULT} respectively.
     *
     * @param line          transfile line to convert into job specification
     * @param transfileName name of parent transfile
     * @param fileStoreId   file-store service ID of data file referenced in transfile line
     * @param rawTransfile  transfile content (can be null)
     * @return JobSpecification instance
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued fileStoreId argument
     */
    public static JobSpecification createJobSpecification(TransFile.Line line, String transfileName, String fileStoreId, byte[] rawTransfile)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(line, "line");
        InvariantUtil.checkNotNullNotEmptyOrThrow(transfileName, "transfileName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileStoreId, "fileStoreId");
        String ccMail = Util.CommandLineOption.CC_MAIL_ADDRESS.get();
        String destination = getFieldValue(line, "b", Constants.MISSING_FIELD_VALUE);

        String defaultPackaging = Constants.MISSING_FIELD_VALUE;
        String defaultEncoding = Constants.MISSING_FIELD_VALUE;
        JobSpecification.Type defaultJobType = JobSpecification.Type.PERSISTENT;
        if (DESTINATION_DANBIB.equals(destination)) {
            defaultPackaging = PACKAGING_DANBIB_DEFAULT;
            defaultEncoding = ENCODING_DANBIB_DEFAULT;
        }

        JobSpecification.Type jobType = defaultJobType;
        if (DESTINATION_MARCKONV.equals(destination)) {
            jobType = JobSpecification.Type.TRANSIENT;
        }
        if (Constants.JOBTYPE_TRANSIENT.equals(getFieldValue(line, "j", Constants.JOBTYPE_PERSISTENT))) {
            jobType = JobSpecification.Type.TRANSIENT;
        }
        if (Constants.JOBTYPE_SUPER_TRANSIENT.equals(getFieldValue(line, "j", Constants.JOBTYPE_PERSISTENT))) {
            jobType = JobSpecification.Type.SUPER_TRANSIENT;
        }


        String packaging = getFieldValue(line, "t", defaultPackaging);
        String format = getFieldValue(line, "o", Constants.MISSING_FIELD_VALUE);
        String encoding = getFieldValue(line, "c", defaultEncoding);

        return new JobSpecification()
                .withPackaging(packaging)
                .withFormat(format)
                .withCharset(encoding)
                .withDestination(destination)
                .withSubmitterId(getSubmitterIdOrMissing(transfileName))
                .withMailForNotificationAboutVerification(addCC(getFieldValue(line, "m", Constants.MISSING_FIELD_VALUE), ccMail))
                .withMailForNotificationAboutProcessing(addCC(getFieldValue(line, "M", Constants.MISSING_FIELD_VALUE), ccMail))
                .withResultmailInitials(getFieldValue(line, "i", Constants.MISSING_FIELD_VALUE))
                .withDataFile(getFileStoreUrnOrMissing(line, fileStoreId))
                .withType(jobType)
                .withAncestry(getAncestry(transfileName, line, rawTransfile));
    }

    private static long getSubmitterIdOrMissing(String transfileName) {
        if (transfileName == null ||
                transfileName.trim().isEmpty() ||
                Constants.MISSING_FIELD_VALUE.equals(transfileName)) {
            return Constants.MISSING_SUBMITTER_VALUE;
        }
        try {
            String submitter = transfileName.substring(0, 6);
            return Long.parseLong(submitter);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return Constants.MISSING_SUBMITTER_VALUE;
        }
    }
    private static String addCC(String address, String cc) {
        if (Constants.MISSING_FIELD_VALUE.equals(address)) {
            return Constants.MISSING_FIELD_VALUE;
        }
        if (address.toLowerCase().contains("dbc.dk")) {
            return address;
        }
        if (cc != null) {
            LOGGER.info("Added cc address:{}", cc);
            return address + ";" + cc;
        } else {
            return address;
        }
    }

    private static String getFieldValue(TransFile.Line line, String fieldName, String defaultValue) {
        String fieldValue = line.getField(fieldName);
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return defaultValue;
        }
        return fieldValue;
    }

    private static String getFileStoreUrnOrMissing(TransFile.Line line, String fileStoreId) throws IllegalArgumentException {
        String fieldValue = getFieldValue(line, "f", Constants.MISSING_FIELD_VALUE);
        if (Constants.MISSING_FIELD_VALUE.equals(fieldValue)) {
            return Constants.MISSING_FIELD_VALUE;
        }
        if (Constants.MISSING_FIELD_VALUE.equals(fileStoreId)) {
            return Constants.MISSING_FIELD_VALUE;
        }
        return FileStoreUrn.create(fileStoreId).toString();
    }

    private static JobSpecification.Ancestry getAncestry(String transfileName, TransFile.Line line, byte[] rawTransfile) {
        String datafileName = getFieldValue(line, "f", Constants.MISSING_FIELD_VALUE);
        String batchId = getBatchId(datafileName);
        return new JobSpecification.Ancestry()
                .withTransfile(transfileName)
                .withDatafile(datafileName)
                .withBatchId(batchId)
                .withDetails(rawTransfile);
    }

    private static String getBatchId(String datafileName) {
        String[] split = datafileName.split("\\.");
        if (split.length > 2) {
            return split[1];
        }
        return null;
    }
}
