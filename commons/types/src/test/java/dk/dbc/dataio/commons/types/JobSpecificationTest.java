package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
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
    private static final JobSpecification.Type TYPE = JobSpecification.Type.TEST;

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

    @Test
    public void constructor_typeArgIsNull_returnsNewJobSpecificationInstance() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, null);
    }


    public static JobSpecification newJobSpecificationInstance() {
        return new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE, TYPE);
    }

    @Test
    public void testJsonUnmarshalling() throws Exception {

        String data =
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

        final JobSpecification jobSpecification = JsonUtil.fromJson(data, JobSpecification.class);
        assertThat(jobSpecification.getPackaging(), is("packaging"));
        assertThat(jobSpecification.getType(), is(TYPE));
    }

}
