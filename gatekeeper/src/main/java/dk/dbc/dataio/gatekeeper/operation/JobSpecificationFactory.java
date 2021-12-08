/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;

/**
 * Factory class for the creation of job specifications from trans file entries
 */
public class JobSpecificationFactory {
    public static final String DESTINATION_MARCKONV = "marckonv";
    public static final String DESTINATION_DANBIB = "danbib";
    public static final String PACKAGING_DANBIB_DEFAULT = "iso";
    public static final String ENCODING_DANBIB_DEFAULT = "latin-1";

    private JobSpecificationFactory() {}

    /**
     * Creates job specification from given transfile line using
     * "missing value" placeholders for missing field values. In case destination
     * is {@value #DESTINATION_DANBIB} missing values for packaging and/or
     * encoding fields will be set to {@value PACKAGING_DANBIB_DEFAULT} and
     * {@value ENCODING_DANBIB_DEFAULT} respectively.
     * @param line transfile line to convert into job specification
     * @param transfileName name of parent transfile
     * @param fileStoreId file-store service ID of data file referenced in transfile line
     * @param rawTransfile transfile content (can be null)
     * @return JobSpecification instance
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued fileStoreId argument
     */
    public static JobSpecification createJobSpecification(TransFile.Line line, String transfileName, String fileStoreId, byte[] rawTransfile)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(line, "line");
        InvariantUtil.checkNotNullNotEmptyOrThrow(transfileName, "transfileName");
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileStoreId, "fileStoreId");

        final String destination = getFieldValue(line, "b", Constants.MISSING_FIELD_VALUE);

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

        final String packaging = getFieldValue(line, "t", defaultPackaging);
        final String format = getFieldValue(line, "o", Constants.MISSING_FIELD_VALUE);
        final String encoding = getFieldValue(line, "c", defaultEncoding);

        return new JobSpecification()
                .withPackaging(packaging)
                .withFormat(format)
                .withCharset(encoding)
                .withDestination(destination)
                .withSubmitterId(getSubmitterIdOrMissing(transfileName))
                .withMailForNotificationAboutVerification(getFieldValue(line, "m", Constants.MISSING_FIELD_VALUE))
                .withMailForNotificationAboutProcessing(getFieldValue(line, "M", Constants.MISSING_FIELD_VALUE))
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
            final String submitter = transfileName.substring(0, 6);
            return Long.parseLong(submitter);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return Constants.MISSING_SUBMITTER_VALUE;
        }
    }

    private static String getFieldValue(TransFile.Line line, String fieldName, String defaultValue) {
        final String fieldValue = line.getField(fieldName);
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return defaultValue;
        }
        return fieldValue;
    }

    private static String getFileStoreUrnOrMissing(TransFile.Line line, String fileStoreId) throws IllegalArgumentException {
        final String fieldValue = getFieldValue(line, "f", Constants.MISSING_FIELD_VALUE);
        if (Constants.MISSING_FIELD_VALUE.equals(fieldValue)) {
            return Constants.MISSING_FIELD_VALUE;
        }
        if (Constants.MISSING_FIELD_VALUE.equals(fileStoreId)) {
            return Constants.MISSING_FIELD_VALUE;
        }
        return FileStoreUrn.create(fileStoreId).toString();
    }

    private static JobSpecification.Ancestry getAncestry(String transfileName, TransFile.Line line, byte[] rawTransfile) {
        final String datafileName = getFieldValue(line, "f", Constants.MISSING_FIELD_VALUE);
        final String batchId = getBatchId(datafileName);
        return new JobSpecification.Ancestry()
                .withTransfile(transfileName)
                .withDatafile(datafileName)
                .withBatchId(batchId)
                .withDetails(rawTransfile);
    }

    private static String getBatchId(String datafileName) {
        final String[] split = datafileName.split("\\.");
        if (split.length > 2) {
            return split[1];
        }
        return null;
    }
}
