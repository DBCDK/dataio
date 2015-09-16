package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobSpecificationFactoryTest {
    private final String transfileName = "123456.trans";

    @Test(expected = NullPointerException.class)
    public void createJobSpecification_lineArgIsNull_throws() {
        JobSpecificationFactory.createJobSpecification(null, transfileName, "42");
    }

    @Test(expected = NullPointerException.class)
    public void createJobSpecification_fileStoreIdArgIsNull_throws() {
        JobSpecificationFactory.createJobSpecification(new TransFile.Line("foo"), transfileName, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createJobSpecification_fileStoreIdArgIsEmpty_throws() {
        JobSpecificationFactory.createJobSpecification(new TransFile.Line("foo"), transfileName, " ");
    }

    @Test(expected = NullPointerException.class)
    public void createJobSpecification_transfileNameArgIsNull_throws() {
        JobSpecificationFactory.createJobSpecification(new TransFile.Line("foo"), null, "42");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createJobSpecification_transfileNameArgArgIsEmpty_throws() {
        JobSpecificationFactory.createJobSpecification(new TransFile.Line("foo"), " ", "42");
    }

    @Test
    public void createJobSpecification_lineArgIsValid_mapsFieldsToJobSpecification() {
        final int submitter = 810010;
        final String batchId = "001";
        final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder()
                .setSubmitterId(submitter)
                .setDataFile("urn:dataio-fs:42")
                .setType(JobSpecification.Type.PERSISTENT)
                .setAncestry(
                        new JobSpecificationBuilder.AncestryBuilder()
                                .setTransfile(transfileName)
                                .setDatafile(String.format("%d.%s.dat", submitter, batchId))
                                .setBatchId(batchId)
                                .build()
                )
                .build();
        final String rawTransfileLine = String.format("b=%s,f=%d.%s.dat,t=%s,c=%s,o=%s,m=%s,M=%s,i=%s",
                jobSpecificationTemplate.getDestination(),
                submitter,
                batchId,
                jobSpecificationTemplate.getPackaging(),
                jobSpecificationTemplate.getCharset(),
                jobSpecificationTemplate.getFormat(),
                jobSpecificationTemplate.getMailForNotificationAboutVerification(),
                jobSpecificationTemplate.getMailForNotificationAboutProcessing(),
                jobSpecificationTemplate.getResultmailInitials());

        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line(rawTransfileLine), transfileName, "42");

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_lineArgHasMissingFields_mapsFieldsToJobSpecification() {
        final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder()
                .setDestination(Constants.MISSING_FIELD_VALUE)
                .setSubmitterId(Constants.MISSING_SUBMITTER_VALUE)
                .setPackaging(Constants.MISSING_FIELD_VALUE)
                .setCharset(Constants.MISSING_FIELD_VALUE)
                .setFormat(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .setResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .setDataFile(Constants.MISSING_FIELD_VALUE)
                .setType(JobSpecification.Type.PERSISTENT)
                .setAncestry(
                        new JobSpecificationBuilder.AncestryBuilder()
                                .setTransfile(transfileName)
                                .setDatafile(Constants.MISSING_FIELD_VALUE)
                                .build()
                )
                .build();
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("foo"), transfileName, "42");

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_lineArgHasEmptyFields_mapsFieldsToJobSpecification() {
        final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder()
                .setDestination(Constants.MISSING_FIELD_VALUE)
                .setSubmitterId(Constants.MISSING_SUBMITTER_VALUE)
                .setPackaging(Constants.MISSING_FIELD_VALUE)
                .setCharset(Constants.MISSING_FIELD_VALUE)
                .setFormat(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .setResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .setDataFile(Constants.MISSING_FIELD_VALUE)
                .setType(JobSpecification.Type.PERSISTENT)
                .setAncestry(
                        new JobSpecificationBuilder.AncestryBuilder()
                                .setTransfile(transfileName)
                                .setDatafile(Constants.MISSING_FIELD_VALUE)
                                .build()
                )
                .build();
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b= ,f=,t= ,c=,o= ,m=,M= ,i="), transfileName, "42");

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_fileFieldSubstringOutOfBounds_mapsToMissingField() {
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("f=123"), transfileName, "42");
        assertThat(jobSpecification.getSubmitterId(), is(Constants.MISSING_SUBMITTER_VALUE));
    }

    @Test
    public void createJobSpecification_fileFieldSubstringNaN_mapsToMissingField() {
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("f=abcdefhijklmnopq"), transfileName, "42");
        assertThat(jobSpecification.getSubmitterId(), is(Constants.MISSING_SUBMITTER_VALUE));
    }

    @Test
    public void createJobSpecification_fileStoreIdArgIsMissing_mapsToOriginalFieldValue() {
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("f=123456.file"), transfileName, Constants.MISSING_FIELD_VALUE);
        assertThat(jobSpecification.getDataFile(), is("123456.file"));
    }
}