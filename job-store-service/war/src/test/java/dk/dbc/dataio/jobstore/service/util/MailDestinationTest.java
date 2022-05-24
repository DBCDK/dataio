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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.vipcore.exception.VipCoreException;
import dk.dbc.vipcore.marshallers.Information;
import dk.dbc.vipcore.service.VipCoreServiceConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Properties;

import static dk.dbc.dataio.jobstore.service.ejb.JobNotificationRepositoryTest.getNotificationEntity;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MailDestinationTest {
    private final String mailToFallback = "default@dbc.dk";
    private final VipCoreServiceConnector vipCoreServiceConnector = mock(VipCoreServiceConnector.class);

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void toString_notificationWithoutJobSpecificationWithNullDestination_returnsFallback() {
        environmentVariables.set("MAIL_TO_FALLBACK", mailToFallback);
        final NotificationEntity notification = createNotificationEntity();
        notification.setDestination(null);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(mailToFallback));
    }

    @Test
    public void toString_notificationWithoutJobSpecificationWithEmptyDestination_returnsFallback() {
        environmentVariables.set("MAIL_TO_FALLBACK", mailToFallback);
        final NotificationEntity notification = createNotificationEntity();
        notification.setDestination(" ");

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(mailToFallback));
    }

    @Test
    public void toString_notificationWithoutJobSpecificationWithMissingDestination_returnsFallback() {
        environmentVariables.set("MAIL_TO_FALLBACK", mailToFallback);
        final NotificationEntity notification = createNotificationEntity();
        notification.setDestination(Constants.MISSING_FIELD_VALUE);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(mailToFallback));
    }

    @Test
    public void toString_notificationForJobSpecificationWithEmptyDestination_returnsFallback() {
        environmentVariables.set("MAIL_TO_FALLBACK", mailToFallback);
        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification(" ");
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(mailToFallback));
    }

    @Test
    public void toString_notificationForJobSpecificationWithMissingDestination_returnsFallback() {
        environmentVariables.set("MAIL_TO_FALLBACK", mailToFallback);
        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE);
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(mailToFallback));
    }

    @Test
    public void toString_notificationForTypeJobCreated_returnsMailForNotificationAboutVerification() {
        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification("verification@company.com")
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE);
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(jobSpecification.getMailForNotificationAboutVerification()));
    }

    @Test
    public void toString_notificationForTypeJobCompleted_returnsMailForNotificationAboutProcessing() {
        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutProcessing("processing@company.com");
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_COMPLETED, jobSpecification);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(jobSpecification.getMailForNotificationAboutProcessing()));
    }

    @Test
    public void toString_notificationForTypeJobCompletedWithoutMailForNotificationAboutProcessing_returnsMailForNotificationAboutVerification() {
        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification("verification@company.com")
                .withMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE);
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_COMPLETED, jobSpecification);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(jobSpecification.getMailForNotificationAboutVerification()));
    }

    @Test
    public void toString_notificationWithOpenAgencyCallWhenAgencyInformationPropertyIsNull_returnsFallback() {
        environmentVariables.set("MAIL_TO_FALLBACK", mailToFallback);
        setOpenAgencyConnectorExpectation(new Information());

        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification(Constants.CALL_OPEN_AGENCY)
                .withMailForNotificationAboutProcessing("processing@company.com");
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(mailToFallback));
    }

    @Test
    public void toString_notificationWithOpenAgencyCallWhenAgencyInformationPropertyIsEmpty_returnsFallback() {
        environmentVariables.set("MAIL_TO_FALLBACK", mailToFallback);
        final Information agencyInformation = new Information();
        agencyInformation.setBranchTransReportEmail("  ");
        setOpenAgencyConnectorExpectation(agencyInformation);

        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification(Constants.CALL_OPEN_AGENCY)
                .withMailForNotificationAboutProcessing("processing@company.com");
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(mailToFallback));
    }

    @Test
    public void toString_notificationForTypeJobCreatedWithOpenAgencyCall_returnsBranchTransReportEmail() {
        final Information agencyInformation = new Information();
        agencyInformation.setBranchTransReportEmail("mail@company.com");
        setOpenAgencyConnectorExpectation(agencyInformation);

        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification(Constants.CALL_OPEN_AGENCY)
                .withMailForNotificationAboutProcessing("processing@company.com");
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(agencyInformation.getBranchTransReportEmail()));
    }

    @Test
    public void toString_notificationForTypeJobCompletedWithOpenAgencyCall_returnsBranchRejectedRecordsEmail() {
        final Information agencyInformation = new Information();
        agencyInformation.setBranchRejectedRecordsEmail("mail@company.com");
        setOpenAgencyConnectorExpectation(agencyInformation);

        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification("verification@company.com")
                .withMailForNotificationAboutProcessing(Constants.CALL_OPEN_AGENCY);
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_COMPLETED, jobSpecification);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(agencyInformation.getBranchRejectedRecordsEmail()));
    }

    @Test
    public void toString_notificationForTypeJobCompletedWithOpenAgencyCallWithoutBranchRejectedRecordsEmail_returnsBranchTransReportMail() {
        final Information agencyInformation = new Information();
        agencyInformation.setBranchTransReportEmail("mail@company.com");
        setOpenAgencyConnectorExpectation(agencyInformation);

        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutProcessing(Constants.CALL_OPEN_AGENCY);
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_COMPLETED, jobSpecification);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.toString(), is(agencyInformation.getBranchTransReportEmail()));
    }

    @Test
    public void toAddresses() throws AddressException {
        environmentVariables.set("MAIL_TO_FALLBACK", mailToFallback);
        final NotificationEntity notification = createNotificationEntity();
        notification.setDestination(null);

        final MailDestination mailDestination = createMailDestination(notification);
        assertThat(mailDestination.getToAddresses(), is(new InternetAddress[]{new InternetAddress(mailToFallback)}));
    }

    @Test
    public void getMailSession() {
        final Session mailSession = Session.getDefaultInstance(new Properties());
        final MailDestination mailDestination = new MailDestination(mailSession, createNotificationEntity(), vipCoreServiceConnector);
        assertThat(mailDestination.getMailSession(), is(mailSession));
    }

    private MailDestination createMailDestination(NotificationEntity notification) {
        return new MailDestination(Session.getDefaultInstance(
                new Properties()), notification, vipCoreServiceConnector);
    }

    private NotificationEntity createNotificationEntity() {
        return getNotificationEntity(Notification.Type.INVALID_TRANSFILE, new JobEntity());
    }

    public void setOpenAgencyConnectorExpectation(Information agencyInformation) {
        try {
            when(vipCoreServiceConnector.getInformation(any())).thenReturn(agencyInformation);
        } catch (VipCoreException e) {
            throw new IllegalStateException(e);
        }
    }
}
