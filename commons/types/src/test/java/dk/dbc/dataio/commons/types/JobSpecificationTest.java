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

package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * JobSpecification unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class JobSpecificationTest {
    private static final String PACKAGING = "packaging";
    private static final String FORMAT = "format";
    private static final String CHARSET = "charset";
    private static final String DESTINATION = "destination";
    private static final Long SUBMITTER_ID = 42L;
    private static final String VERIFICATION_MAILADDR = "verify@dbc.dk";
    private static final String PROCESSING_MAILADDR = "processing@dbc.dk";
    private static final String RESULT_MAIL_INITIALS = "abc";
    private static final String DATA_FILE = "uri";
    private static final String TRANS_FILE = "820040.stark.trans";
    private static final JobSpecification.Type TYPE = JobSpecification.Type.TEST;
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test(expected = NullPointerException.class)
    public void constructor_packagingArgIsNull_throws() {
        new JobSpecification(null, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_packagingArgIsEmpty_throws() {
        new JobSpecification("", FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_formatArgIsNull_throws() {
        new JobSpecification(PACKAGING, null, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_formatArgIsEmpty_throws() {
        new JobSpecification(PACKAGING, "", CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_charsetArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, null, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_charsetArgIsEmpty_throws() {
        new JobSpecification(PACKAGING, FORMAT, "", DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_destinationArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, null, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_destinationArgIsEmpty_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, "", SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_flowIdArgIsLessThanLowerBound_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, Constants.PERSISTENCE_ID_LOWER_BOUND - 1, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_verificationMailArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, null, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_processingMailArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, null, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_resultmailInitialsArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, null, DATA_FILE, TYPE);
    }

    // Insert new tests here

    @Test(expected = NullPointerException.class)
    public void constructor_dateFileArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, null, TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_dateFileArgIsEmpty_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, "", TYPE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_typeArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, null);
    }

    @Test
    public void hasNotificationDestination_returnsTrueOnAnyNonEmptyMailAddr() {
        JobSpecification jobSpecification = new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, "", RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
        assertThat("mailForNotificationAboutVerification is non-empty", jobSpecification.hasNotificationDestination(), is(true));
        jobSpecification = new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, "", PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
        assertThat("mailForNotificationAboutProcessing is non-empty", jobSpecification.hasNotificationDestination(), is(true));
        jobSpecification = new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
        assertThat("Both are non-empty", jobSpecification.hasNotificationDestination(), is(true));
    }

    @Test
    public void hasNotificationDestination_returnsFalseOnEmptyMailAddr() {
        final JobSpecification jobSpecification = new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, "  ", "  ", RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
        assertThat(jobSpecification.hasNotificationDestination(), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_ancestryHasNullValuedDataFile_throws() {
        new JobSpecification.Ancestry(TRANS_FILE, null, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_ancestryHasEmptyValuedDataFile_throws() {
        new JobSpecification.Ancestry(TRANS_FILE, "", "");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_ancestryHasNullValuedTransFile_throws() {
        new JobSpecification.Ancestry(null, DATA_FILE, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_ancestryHasEmptyValuedTransFile_throws() {
        new JobSpecification.Ancestry("", DATA_FILE, "");
    }

    public static JobSpecification newJobSpecificationInstance() {
        return new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test
    public void testJsonUnmarshallingWithoutAncestry() throws Exception {
        final String json =
                "{ \"packaging\" : \"packaging\", \n" +
                    "    \"format\" : \"format\", \n" +
                    "    \"charset\" : \"charset\", \n" +
                    "    \"destination\" : \"destination\", \n" +
                    "    \"submitterId\" : 42, \n" +
                    "    \"mailForNotificationAboutVerification\" : \"ab@cd.ef\", \n" +
                    "    \"mailForNotificationAboutProcessing\" : \"ab@cd.ef\", \n" +
                    "    \"resultmailInitials\" : \"abc\", \n" +
                    "    \"dataFile\" : \"dataFile\", \n" +
                    "    \"type\" : \"TEST\"" +
                "}";

        final JobSpecification jobSpecification = jsonbContext.unmarshall(json, JobSpecification.class);
        assertThat(jobSpecification.getPackaging(), is("packaging"));
        assertThat(jobSpecification.getType(), is(TYPE));
        assertThat(jobSpecification.getAncestry(), is(nullValue()));
    }

    @Test
    public void testJsonUnmarshallingWithAncestry() throws Exception {
        final String json =
                "{\n" +
                    "    \"packaging\" : \"packaging\",\n" +
                    "    \"format\" : \"format\",\n" +
                    "    \"charset\" : \"charset\",\n" +
                    "    \"destination\" : \"destination\",\n" +
                    "    \"submitterId\" : 42,\n" +
                    "    \"mailForNotificationAboutVerification\" : \"ab@cd.ef\",\n" +
                    "    \"mailForNotificationAboutProcessing\" : \"ab@cd.ef\",\n" +
                    "    \"resultmailInitials\" : \"abc\",\n" +
                    "    \"dataFile\" : \"dataFile\",\n" +
                    "    \"type\" : \"TEST\",\n" +
                    "    \"ancestry\" : {\n" +
                    "        \"transfile\" : \"file.trans\",\n" +
                    "        \"datafile\" : \"file.dat\",\n" +
                    "        \"batchId\" : \"01\"\n" +
                    "    }\n" +
                "}";

        final JobSpecification jobSpecification = jsonbContext.unmarshall(json, JobSpecification.class);
        assertThat(jobSpecification.getPackaging(), is("packaging"));
        assertThat(jobSpecification.getType(), is(TYPE));
        final JobSpecification.Ancestry ancestry = jobSpecification.getAncestry();
        assertThat(ancestry, is(notNullValue()));
        assertThat(ancestry.getTransfile(), is("file.trans"));
        assertThat(ancestry.getDatafile(), is("file.dat"));
        assertThat(ancestry.getBatchId(), is("01"));
    }
}
