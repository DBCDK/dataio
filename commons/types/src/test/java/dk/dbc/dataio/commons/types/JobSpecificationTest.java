package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * JobSpecification unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
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
    private static final JobSpecification.Type TYPE = JobSpecification.Type.TEST;
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void constructor_packagingArgIsNull_throws() {
        assertThat(() -> newJobSpecificationInstance().withPackaging(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_packagingArgIsEmpty_throws() {
        assertThat(() -> newJobSpecificationInstance().withPackaging(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void constructor_formatArgIsNull_throws() {
        assertThat(() -> newJobSpecificationInstance().withFormat(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_formatArgIsEmpty_throws() {
        assertThat(() -> newJobSpecificationInstance().withFormat(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void constructor_charsetArgIsNull_throws() {
        assertThat(() -> newJobSpecificationInstance().withCharset(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_charsetArgIsEmpty_throws() {
        assertThat(() -> newJobSpecificationInstance().withCharset(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void constructor_destinationArgIsNull_throws() {
        assertThat(() -> newJobSpecificationInstance().withDestination(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_destinationArgIsEmpty_throws() {
        assertThat(() -> newJobSpecificationInstance().withDestination(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void constructor_submitterIdArgIsLessThanLowerBound_throws() {
        assertThat(() -> newJobSpecificationInstance().withSubmitterId(0), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void constructor_verificationMailArgIsNull_throws() {
        assertThat(() -> newJobSpecificationInstance().withMailForNotificationAboutVerification(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_processingMailArgIsNull_throws() {
        assertThat(() -> newJobSpecificationInstance().withMailForNotificationAboutProcessing(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_resultmailInitialsArgIsNull_throws() {
        assertThat(() -> newJobSpecificationInstance().withResultmailInitials(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_dateFileArgIsNull_throws() {
        assertThat(() -> newJobSpecificationInstance().withDataFile(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_dateFileArgIsEmpty_throws() {
        assertThat(() -> newJobSpecificationInstance().withDataFile(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void constructor_typeArgIsNull_throws() {
        assertThat(() -> newJobSpecificationInstance().withType(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void hasNotificationDestination_returnsTrueOnAnyNonEmptyMailAddr() {
        JobSpecification jobSpecification = newJobSpecificationInstance().withMailForNotificationAboutProcessing("");
        assertThat("mailForNotificationAboutVerification is non-empty", jobSpecification.hasNotificationDestination(), is(true));
        jobSpecification = newJobSpecificationInstance().withMailForNotificationAboutVerification("");
        assertThat("mailForNotificationAboutProcessing is non-empty", jobSpecification.hasNotificationDestination(), is(true));
        jobSpecification = newJobSpecificationInstance();
        assertThat("Both are non-empty", jobSpecification.hasNotificationDestination(), is(true));
    }

    @Test
    public void hasNotificationDestination_returnsFalseOnEmptyMailAddr() {
        final JobSpecification jobSpecification = newJobSpecificationInstance().withMailForNotificationAboutVerification("  ").withMailForNotificationAboutProcessing("  ");
        assertThat(jobSpecification.hasNotificationDestination(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ancestryWithEmptyValuedTransFile_throws() {
        new JobSpecification.Ancestry()
                .withTransfile(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ancestryWithEmptyValuedDataFile_throws() {
        new JobSpecification.Ancestry()
                .withDatafile(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ancestryWithEmptyValuedHarvesterToken_throws() {
        new JobSpecification.Ancestry()
                .withHarvesterToken(" ");
    }

    public static JobSpecification newJobSpecificationInstance() {
        return new JobSpecification().withPackaging(PACKAGING).withFormat(FORMAT).withCharset(CHARSET).withDestination(DESTINATION).withSubmitterId(SUBMITTER_ID).withMailForNotificationAboutVerification(VERIFICATION_MAILADDR)
                .withMailForNotificationAboutProcessing(PROCESSING_MAILADDR).withResultmailInitials(RESULT_MAIL_INITIALS).withDataFile(DATA_FILE).withType(TYPE);
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
