package dk.dbc.dataio.commons.utils.jobstore.transfile;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static dk.dbc.dataio.commons.types.JobSpecification.Type.PERSISTENT;
import static dk.dbc.dataio.commons.types.JobSpecification.Type.SUPER_TRANSIENT;
import static dk.dbc.dataio.commons.types.JobSpecification.Type.TRANSIENT;

/**
 * Factory class for the creation of job specifications from trans file entries
 */
public class JobSpecificationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSpecificationFactory.class);
    public static final String DESTINATION_MARCKONV = "marckonv";
    public static final String DESTINATION_DANBIB = "danbib";
    public static final String PACKAGING_DANBIB_DEFAULT = "iso";
    public static final String ENCODING_DANBIB_DEFAULT = "latin-1";
    public static Supplier<String> CC_MAIL = () -> System.getenv("CC_MAIL");

    /**
     * Creates job specification from given transfile map using
     * "missing value" placeholders for missing field values. In case destination
     * is {@value #DESTINATION_DANBIB} missing values for packaging and/or
     * encoding fields will be set to {@value PACKAGING_DANBIB_DEFAULT} and
     * {@value ENCODING_DANBIB_DEFAULT} respectively.
     *
     * @param map          transfile map to convert into job specification
     * @param transfileName name of parent transfile
     * @param fileStoreId   file-store service ID of data file referenced in transfile map
     * @param rawTransfile  transfile content (can be null)
     * @return JobSpecification instance
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued fileStoreId argument
     */
    public static JobSpecification createJobSpecification(Map<Character, String> map, String transfileName, String fileStoreId, byte[] rawTransfile)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(map, "map");
        InvariantUtil.checkNotNullNotEmptyOrThrow(transfileName, "transfileName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileStoreId, "fileStoreId");
        String destination = map.getOrDefault('b', Constants.MISSING_FIELD_VALUE);

        String defaultPackaging = Constants.MISSING_FIELD_VALUE;
        String defaultEncoding = Constants.MISSING_FIELD_VALUE;
        if (DESTINATION_DANBIB.equals(destination)) {
            defaultPackaging = PACKAGING_DANBIB_DEFAULT;
            defaultEncoding = ENCODING_DANBIB_DEFAULT;
        }

        JobSpecification.Type jobType = DESTINATION_MARCKONV.equals(destination) ? TRANSIENT : PERSISTENT;
        if (Constants.JOBTYPE_TRANSIENT.equals(map.get('j'))) jobType = TRANSIENT;
        if (Constants.JOBTYPE_SUPER_TRANSIENT.equals(map.get('j'))) jobType = SUPER_TRANSIENT;


        String packaging = map.getOrDefault('t', defaultPackaging);
        String format = map.getOrDefault('o', Constants.MISSING_FIELD_VALUE);
        String encoding = map.getOrDefault('c', defaultEncoding);

        return new JobSpecification()
                .withPackaging(packaging)
                .withFormat(format)
                .withCharset(encoding)
                .withDestination(destination)
                .withSubmitterId(getSubmitterIdOrMissing(transfileName))
                .withMailForNotificationAboutVerification(addCC(map.getOrDefault('m', Constants.MISSING_FIELD_VALUE), CC_MAIL.get()))
                .withMailForNotificationAboutProcessing(addCC(map.getOrDefault('M', Constants.MISSING_FIELD_VALUE), CC_MAIL.get()))
                .withResultmailInitials(map.getOrDefault('i', Constants.MISSING_FIELD_VALUE))
                .withDataFile(getFileStoreUrnOrMissing(map, fileStoreId))
                .withType(jobType)
                .withAncestry(getAncestry(transfileName, map, rawTransfile));
    }

    public static Map<Character, String> transfileLineToMap(String transfileLine) {
        String[] pairs = transfileLine.split(",");
        Map<Character, String> map = new HashMap<>();
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if(kv.length == 2 && !kv[1].isBlank()) {
                String key = kv[0].trim();
                if(key.length() > 1) throw new IllegalArgumentException("Field key can only be one character: " + key);
                map.put(key.charAt(0), kv[1].trim());
            } else LOGGER.warn("Invalid field " + pair);
        }
        return map;
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

    private static String getFileStoreUrnOrMissing(Map<Character, String> line, String fileStoreId) throws IllegalArgumentException {
        String fieldValue = line.getOrDefault('f', Constants.MISSING_FIELD_VALUE);
        if (Constants.MISSING_FIELD_VALUE.equals(fieldValue)) {
            return Constants.MISSING_FIELD_VALUE;
        }
        if (Constants.MISSING_FIELD_VALUE.equals(fileStoreId)) {
            return Constants.MISSING_FIELD_VALUE;
        }
        return FileStoreUrn.create(fileStoreId).toString();
    }

    private static JobSpecification.Ancestry getAncestry(String transfileName, Map<Character, String> line, byte[] rawTransfile) {
        String datafileName = line.getOrDefault('f', Constants.MISSING_FIELD_VALUE);
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
