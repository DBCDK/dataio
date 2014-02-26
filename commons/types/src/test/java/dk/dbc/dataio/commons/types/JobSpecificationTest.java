package dk.dbc.dataio.commons.types;

import org.junit.Test;

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

    @Test(expected = NullPointerException.class)
    public void constructor_packagingArgIsNull_throws() {
        new JobSpecification(null, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_packagingArgIsEmpty_throws() {
        new JobSpecification("", FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_formatArgIsNull_throws() {
        new JobSpecification(PACKAGING, null, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_formatArgIsEmpty_throws() {
        new JobSpecification(PACKAGING, "", CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_charsetArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, null, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_charsetArgIsEmpty_throws() {
        new JobSpecification(PACKAGING, FORMAT, "", DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_destinationArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, null, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_destinationArgIsEmpty_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, "", SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_flowIdArgIsBelowThreshold_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, Constants.PERSISTENCE_ID_LOWER_BOUND, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_verificationMailArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, null, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_processingMailArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, null, RESULT_MAIL_INITIALS, DATA_FILE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_resultmailInitialsArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, null, DATA_FILE);
    }
    // Insert new tetst here

    @Test(expected = NullPointerException.class)
    public void constructor_dateFileArgIsNull_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_dateFileArgIsEmpty_throws() {
        new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, "");
    }

    public static JobSpecification newJobSpecificationInstance() {
        return new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, SUBMITTER_ID, VERIFICATION_MAILADDR, PROCESSING_MAILADDR, RESULT_MAIL_INITIALS, DATA_FILE);
    }
}
