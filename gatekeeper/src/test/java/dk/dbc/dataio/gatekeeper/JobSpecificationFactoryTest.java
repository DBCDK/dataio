package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gatekeeper.operation.JobSpecificationFactory;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobSpecificationFactoryTest {
    private final long submitter = 810010;
    private final String transfileName = String.format("%d.trans", submitter);
    private final byte[] rawTransfile = "content".getBytes(StandardCharsets.UTF_8);


    @Test(expected = NullPointerException.class)
    public void createJobSpecification_lineArgIsNull_throws() {
        JobSpecificationFactory.createJobSpecification(null, transfileName, "42", rawTransfile);
    }

    @Test(expected = NullPointerException.class)
    public void createJobSpecification_fileStoreIdArgIsNull_throws() {
        JobSpecificationFactory.createJobSpecification(new TransFile.Line("foo"), transfileName, null, rawTransfile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createJobSpecification_fileStoreIdArgIsEmpty_throws() {
        JobSpecificationFactory.createJobSpecification(new TransFile.Line("foo"), transfileName, " ", rawTransfile);
    }

    @Test(expected = NullPointerException.class)
    public void createJobSpecification_transfileNameArgIsNull_throws() {
        JobSpecificationFactory.createJobSpecification(new TransFile.Line("foo"), null, "42", rawTransfile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createJobSpecification_transfileNameArgArgIsEmpty_throws() {
        JobSpecificationFactory.createJobSpecification(new TransFile.Line("foo"), " ", "42", rawTransfile);
    }

    @Test
    public void createJobSpecification_rawTransfileArgIsNull_createsJobSpecification() {
        JobSpecificationFactory.createJobSpecification(new TransFile.Line("foo"), "123456.trans", "42", rawTransfile);
    }

    @Test
    public void createJobSpecification_lineArgIsValid_mapsFieldsToJobSpecification() {
        String batchId = "001";
        JobSpecification jobSpecificationTemplate = getJobspec(batchId);

        String rawTransfileLine = getRawTransfileLine(jobSpecificationTemplate, batchId);

        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line(rawTransfileLine), transfileName, "42", rawTransfile);

        assertThat("JobSpecification", jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_Add_CC() throws ParseException {
        Util.parseCommandLine(new String[]{"-m dataio@dbc.dk", "-c http://flowstore.svc",
                "-f http://filestore.svc", "-j http://jobstore.svc", "-d ftpdatadir"});
        String batchId = "001";
        JobSpecification jobSpecificationTemplate = getJobspec(batchId);

        String rawTransfileLine = getRawTransfileLine(jobSpecificationTemplate, batchId);

        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line(rawTransfileLine), transfileName, "42", rawTransfile);

        jobSpecificationTemplate
                .withMailForNotificationAboutVerification("verification@company.com; dataio@dbc.dk")
                .withMailForNotificationAboutProcessing("processing@company.com; dataio@dbc.dk");

        assertThat("JobSpecification", jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_Skip_CC_if_dbc_mail() throws ParseException {
        Util.parseCommandLine(new String[]{"-m dataio@dbc.dk","-c http://flowstore.svc",
                "-f http://filestore.svc", "-j http://jobstore.svc", "-d ftpdatadir"});
        String batchId = "001";
        JobSpecification jobSpecificationTemplate = getJobspec(batchId)
                .withMailForNotificationAboutVerification("verify_dataio@dbc.dk")
                .withMailForNotificationAboutProcessing("processing_dataio@dbc.dk");


        String rawTransfileLine = getRawTransfileLine(jobSpecificationTemplate, batchId);

        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line(rawTransfileLine), transfileName, "42", rawTransfile);

        assertThat("JobSpecification", jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_Skip_CC() throws ParseException {
        Util.parseCommandLine(new String[]{"-c http://flowstore.svc",
                "-f http://filestore.svc", "-j http://jobstore.svc", "-d ftpdatadir"});
        String batchId = "001";
        JobSpecification jobSpecificationTemplate = getJobspec(batchId);

        String rawTransfileLine = getRawTransfileLine(jobSpecificationTemplate, batchId);

        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line(rawTransfileLine), transfileName, "42", rawTransfile);


        assertThat("JobSpecification", jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_lineArgHasMissingFields_mapsFieldsToJobSpecification() {
        JobSpecification jobSpecificationTemplate = new JobSpecification()
                .withDestination(Constants.MISSING_FIELD_VALUE)
                .withSubmitterId(submitter)
                .withPackaging(Constants.MISSING_FIELD_VALUE)
                .withCharset(Constants.MISSING_FIELD_VALUE)
                .withFormat(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .withResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .withDataFile(Constants.MISSING_FIELD_VALUE)
                .withType(JobSpecification.Type.TRANSIENT)
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(Constants.MISSING_FIELD_VALUE)
                        .withDetails(rawTransfile)
                );
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("foo,j=TRANSIENT"), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_lineArgHasEmptyFields_mapsFieldsToJobSpecification() {
        JobSpecification jobSpecificationTemplate = new JobSpecification()
                .withDestination(Constants.MISSING_FIELD_VALUE)
                .withSubmitterId(submitter)
                .withPackaging(Constants.MISSING_FIELD_VALUE)
                .withCharset(Constants.MISSING_FIELD_VALUE)
                .withFormat(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .withResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .withDataFile(Constants.MISSING_FIELD_VALUE)
                .withType(JobSpecification.Type.PERSISTENT)
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(Constants.MISSING_FIELD_VALUE)
                        .withDetails(rawTransfile)
                );
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b= ,f=,t= ,c=,o= ,m=,M= ,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_destinationDanbibWithMissingPackagingAndEncoding_usesDefaults() {
        JobSpecification jobSpecificationTemplate = new JobSpecification()
                .withDestination(JobSpecificationFactory.DESTINATION_DANBIB)
                .withSubmitterId(submitter)
                .withPackaging(JobSpecificationFactory.PACKAGING_DANBIB_DEFAULT)
                .withCharset(JobSpecificationFactory.ENCODING_DANBIB_DEFAULT)
                .withFormat(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .withResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .withDataFile(Constants.MISSING_FIELD_VALUE)
                .withType(JobSpecification.Type.PERSISTENT)
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(Constants.MISSING_FIELD_VALUE)
                        .withDetails(rawTransfile)
                );
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=danbib,f=,t=,c=,o=,m=,M=,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_destinationDanbibWithMissingPackaging_usesDefault() {
        JobSpecification jobSpecificationTemplate = new JobSpecification()
                .withDestination(JobSpecificationFactory.DESTINATION_DANBIB)
                .withSubmitterId(submitter)
                .withPackaging(JobSpecificationFactory.PACKAGING_DANBIB_DEFAULT)
                .withCharset("utf8")
                .withFormat(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .withResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .withDataFile(Constants.MISSING_FIELD_VALUE)
                .withType(JobSpecification.Type.PERSISTENT)
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(Constants.MISSING_FIELD_VALUE)
                        .withDetails(rawTransfile)
                );
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=danbib,f=,t=,c=utf8,o=,m=,M=,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_destinationDanbibWithMissingEncoding_usesDefault() {
        JobSpecification jobSpecificationTemplate = new JobSpecification()
                .withDestination(JobSpecificationFactory.DESTINATION_DANBIB)
                .withSubmitterId(submitter)
                .withPackaging("lin")
                .withCharset(JobSpecificationFactory.ENCODING_DANBIB_DEFAULT)
                .withFormat(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .withResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .withDataFile(Constants.MISSING_FIELD_VALUE)
                .withType(JobSpecification.Type.PERSISTENT)
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(Constants.MISSING_FIELD_VALUE)
                        .withDetails(rawTransfile)
                );
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=danbib,f=,t=lin,c=,o=,m=,M=,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_marckonvDestination() {
        JobSpecification jobSpecificationTemplate = new JobSpecification()
                .withDestination(JobSpecificationFactory.DESTINATION_MARCKONV)
                .withSubmitterId(submitter)
                .withPackaging("lin")
                .withCharset(Constants.MISSING_FIELD_VALUE)
                .withFormat(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .withResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .withDataFile(Constants.MISSING_FIELD_VALUE)
                .withType(JobSpecification.Type.TRANSIENT)
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(Constants.MISSING_FIELD_VALUE)
                        .withDetails(rawTransfile)
                );
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=marckonv,f=,t=lin,c=,o=,m=,M=,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_declareTransient() {
        JobSpecification jobSpecificationTemplate = new JobSpecification()
                .withDestination(JobSpecificationFactory.DESTINATION_DANBIB)
                .withSubmitterId(submitter)
                .withPackaging("lin")
                .withCharset(JobSpecificationFactory.ENCODING_DANBIB_DEFAULT)
                .withFormat(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .withResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .withDataFile(Constants.MISSING_FIELD_VALUE)
                .withType(JobSpecification.Type.TRANSIENT)
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(Constants.MISSING_FIELD_VALUE)
                        .withDetails(rawTransfile)
                );
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=danbib,f=,t=lin,c=,o=,m=,M=,i=,j=TRANSIENT"), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_declarePersistent() {
        JobSpecification jobSpecificationTemplate = new JobSpecification()
                .withDestination(JobSpecificationFactory.DESTINATION_DANBIB)
                .withSubmitterId(submitter)
                .withPackaging("lin")
                .withCharset(JobSpecificationFactory.ENCODING_DANBIB_DEFAULT)
                .withFormat(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .withResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .withDataFile(Constants.MISSING_FIELD_VALUE)
                .withType(JobSpecification.Type.SUPER_TRANSIENT)
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(Constants.MISSING_FIELD_VALUE)
                        .withDetails(rawTransfile)
                );
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=danbib,f=,t=lin,c=,o=,m=,M=,i=,j=SUPER_TRANSIENT"), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }


    @Test
    public void createJobSpecification_nonDanbibDestination_noDefaultsUsed() {
        JobSpecification jobSpecificationTemplate = new JobSpecification()
                .withDestination("not_danbib")
                .withSubmitterId(submitter)
                .withPackaging(Constants.MISSING_FIELD_VALUE)
                .withCharset(Constants.MISSING_FIELD_VALUE)
                .withFormat(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .withResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .withDataFile(Constants.MISSING_FIELD_VALUE)
                .withType(JobSpecification.Type.PERSISTENT)
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(Constants.MISSING_FIELD_VALUE)
                        .withDetails(rawTransfile)
                );
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=not_danbib,f=,t=,c=,o=,m=,M=,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_transfileNameSubstringOutOfBounds_mapsToMissingField() {
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("foo"), "123", "42", rawTransfile);
        assertThat(jobSpecification.getSubmitterId(), is(Constants.MISSING_SUBMITTER_VALUE));
    }

    @Test
    public void createJobSpecification_submitterPartOfTransfileNameIsNaN_mapsToMissingField() {
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("foo"), "abcdef.trans", "42", rawTransfile);
        assertThat(jobSpecification.getSubmitterId(), is(Constants.MISSING_SUBMITTER_VALUE));
    }

    @Test
    public void createJobSpecification_fileStoreIdArgIsMissing_mapsToOriginalFieldValue() {
        JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("f=123456.file"), transfileName, Constants.MISSING_FIELD_VALUE, rawTransfile);
        assertThat(jobSpecification.getAncestry().getDatafile(), is("123456.file"));
        assertThat(jobSpecification.getDataFile(), is(Constants.MISSING_FIELD_VALUE));
    }

    private JobSpecification getJobspec(String batchId) {
        return new JobSpecification()
                .withPackaging("packaging")
                .withFormat("format")
                .withCharset("utf8")
                .withDestination("destination")
                .withSubmitterId(submitter)
                .withDataFile("urn:dataio-fs:42")
                .withType(JobSpecification.Type.PERSISTENT)
                .withMailForNotificationAboutVerification("verification@company.com")
                .withMailForNotificationAboutProcessing("processing@company.com")
                .withResultmailInitials("ABC")
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(String.format("%d.%s.dat", submitter, batchId))
                        .withBatchId(batchId)
                        .withDetails(rawTransfile)
                );
    }
    private String getRawTransfileLine(JobSpecification jobSpecificationTemplate, String batchId) {
        return String.format("b=%s,f=%d.%s.dat,t=%s,c=%s,o=%s,m=%s,M=%s,i=%s",
                jobSpecificationTemplate.getDestination(),
                submitter,
                batchId,
                jobSpecificationTemplate.getPackaging(),
                jobSpecificationTemplate.getCharset(),
                jobSpecificationTemplate.getFormat(),
                jobSpecificationTemplate.getMailForNotificationAboutVerification(),
                jobSpecificationTemplate.getMailForNotificationAboutProcessing(),
                jobSpecificationTemplate.getResultmailInitials());
    }
}
