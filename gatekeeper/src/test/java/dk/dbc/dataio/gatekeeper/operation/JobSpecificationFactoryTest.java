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
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
        final String batchId = "001";
        final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder()
                .setSubmitterId(submitter)
                .setDataFile("urn:dataio-fs:42")
                .setType(JobSpecification.Type.PERSISTENT)
                .setMailForNotificationAboutVerification("verification@company.com")
                .setMailForNotificationAboutProcessing("processing@company.com")
                .setResultmailInitials("ABC")
                .setAncestry(new JobSpecification.Ancestry()
                        .withTransfile(transfileName)
                        .withDatafile(String.format("%d.%s.dat", submitter, batchId))
                        .withBatchId(batchId)
                        .withDetails(rawTransfile)
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
                .createJobSpecification(new TransFile.Line(rawTransfileLine), transfileName, "42", rawTransfile);

        assertThat("JobSpecification", jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_lineArgHasMissingFields_mapsFieldsToJobSpecification() {
        final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder()
                .setDestination(Constants.MISSING_FIELD_VALUE)
                .setSubmitterId(submitter)
                .setPackaging(Constants.MISSING_FIELD_VALUE)
                .setCharset(Constants.MISSING_FIELD_VALUE)
                .setFormat(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .setResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .setDataFile(Constants.MISSING_FIELD_VALUE)
                .setType(JobSpecification.Type.PERSISTENT)
                .setAncestry(new JobSpecification.Ancestry()
                                .withTransfile(transfileName)
                                .withDatafile(Constants.MISSING_FIELD_VALUE)
                                .withDetails(rawTransfile)
                )
                .build();
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("foo"), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_lineArgHasEmptyFields_mapsFieldsToJobSpecification() {
        final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder()
                .setDestination(Constants.MISSING_FIELD_VALUE)
                .setSubmitterId(submitter)
                .setPackaging(Constants.MISSING_FIELD_VALUE)
                .setCharset(Constants.MISSING_FIELD_VALUE)
                .setFormat(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .setResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .setDataFile(Constants.MISSING_FIELD_VALUE)
                .setType(JobSpecification.Type.PERSISTENT)
                .setAncestry(new JobSpecification.Ancestry()
                                .withTransfile(transfileName)
                                .withDatafile(Constants.MISSING_FIELD_VALUE)
                                .withDetails(rawTransfile)
                )
                .build();
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b= ,f=,t= ,c=,o= ,m=,M= ,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_destinationDanbibWithMissingPackagingAndEncoding_usesDefaults() {
        final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder()
                .setDestination(JobSpecificationFactory.DESTINATION_DANBIB)
                .setSubmitterId(submitter)
                .setPackaging(JobSpecificationFactory.PACKAGING_DANBIB_DEFAULT)
                .setCharset(JobSpecificationFactory.ENCODING_DANBIB_DEFAULT)
                .setFormat(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .setResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .setDataFile(Constants.MISSING_FIELD_VALUE)
                .setType(JobSpecification.Type.PERSISTENT)
                .setAncestry(new JobSpecification.Ancestry()
                                .withTransfile(transfileName)
                                .withDatafile(Constants.MISSING_FIELD_VALUE)
                                .withDetails(rawTransfile)
                )
                .build();
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=danbib,f=,t=,c=,o=,m=,M=,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_destinationDanbibWithMissingPackaging_usesDefault() {
        final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder()
                .setDestination(JobSpecificationFactory.DESTINATION_DANBIB)
                .setSubmitterId(submitter)
                .setPackaging(JobSpecificationFactory.PACKAGING_DANBIB_DEFAULT)
                .setCharset("utf8")
                .setFormat(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .setResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .setDataFile(Constants.MISSING_FIELD_VALUE)
                .setType(JobSpecification.Type.PERSISTENT)
                .setAncestry(new JobSpecification.Ancestry()
                                .withTransfile(transfileName)
                                .withDatafile(Constants.MISSING_FIELD_VALUE)
                                .withDetails(rawTransfile)
                )
                .build();
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=danbib,f=,t=,c=utf8,o=,m=,M=,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_destinationDanbibWithMissingEncoding_usesDefault() {
        final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder()
                .setDestination(JobSpecificationFactory.DESTINATION_DANBIB)
                .setSubmitterId(submitter)
                .setPackaging("lin")
                .setCharset(JobSpecificationFactory.ENCODING_DANBIB_DEFAULT)
                .setFormat(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .setResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .setDataFile(Constants.MISSING_FIELD_VALUE)
                .setType(JobSpecification.Type.PERSISTENT)
                .setAncestry(new JobSpecification.Ancestry()
                                .withTransfile(transfileName)
                                .withDatafile(Constants.MISSING_FIELD_VALUE)
                                .withDetails(rawTransfile)
                )
                .build();
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=danbib,f=,t=lin,c=,o=,m=,M=,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_nonDanbibDestination_noDefaultsUsed() {
        final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder()
                .setDestination("not_danbib")
                .setSubmitterId(submitter)
                .setPackaging(Constants.MISSING_FIELD_VALUE)
                .setCharset(Constants.MISSING_FIELD_VALUE)
                .setFormat(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .setResultmailInitials(Constants.MISSING_FIELD_VALUE)
                .setDataFile(Constants.MISSING_FIELD_VALUE)
                .setType(JobSpecification.Type.PERSISTENT)
                .setAncestry(new JobSpecification.Ancestry()
                                .withTransfile(transfileName)
                                .withDatafile(Constants.MISSING_FIELD_VALUE)
                                .withDetails(rawTransfile)
                )
                .build();
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("b=not_danbib,f=,t=,c=,o=,m=,M=,i="), transfileName, "42", rawTransfile);

        assertThat(jobSpecification, is(jobSpecificationTemplate));
    }

    @Test
    public void createJobSpecification_transfileNameSubstringOutOfBounds_mapsToMissingField() {
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("foo"), "123", "42", rawTransfile);
        assertThat(jobSpecification.getSubmitterId(), is(Constants.MISSING_SUBMITTER_VALUE));
    }

    @Test
    public void createJobSpecification_submitterPartOfTransfileNameIsNaN_mapsToMissingField() {
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("foo"), "abcdef.trans", "42", rawTransfile);
        assertThat(jobSpecification.getSubmitterId(), is(Constants.MISSING_SUBMITTER_VALUE));
    }

    @Test
    public void createJobSpecification_fileStoreIdArgIsMissing_mapsToOriginalFieldValue() {
        final JobSpecification jobSpecification = JobSpecificationFactory
                .createJobSpecification(new TransFile.Line("f=123456.file"), transfileName, Constants.MISSING_FIELD_VALUE, rawTransfile);
        assertThat(jobSpecification.getAncestry().getDatafile(), is("123456.file"));
        assertThat(jobSpecification.getDataFile(), is(Constants.MISSING_FIELD_VALUE));
    }
}