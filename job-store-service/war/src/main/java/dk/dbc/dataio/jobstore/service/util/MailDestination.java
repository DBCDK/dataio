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

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;

import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Optional;

import static dk.dbc.dataio.commons.types.Constants.MISSING_FIELD_VALUE;

public class MailDestination {
    private final Session mailSession;
    private final NotificationEntity notification;
    private final String destination;

    public MailDestination(Session mailSession, NotificationEntity notification) {
        this.mailSession = mailSession;
        this.notification = notification;
        this.destination = setDestination();
    }

    public InternetAddress[] getToAddresses() throws AddressException {
        return new InternetAddress[]{new InternetAddress(destination)};
    }

    public Session getMailSession() {
        return mailSession;
    }

    @Override
    public String toString() {
        return destination;
    }

    private String setDestination() {
        String destination = notification.getDestination();
        if (MailNotification.isUndefined(destination)) {
           destination = inferDestinationFromJobSpecification().orElse(MISSING_FIELD_VALUE);
           if (destination.equals(MISSING_FIELD_VALUE)) {
               destination = mailSession.getProperty("mail.to.fallback");
           }
        }
        return destination;
    }

    private Optional<String> inferDestinationFromJobSpecification() {
        switch (notification.getType()) {
            case JOB_CREATED:   return getJobCreatedNotificationDestinationFromJobSpecification(notification.getJob().getSpecification());
            case JOB_COMPLETED: return getJobCompletedNotificationDestinationFromJobSpecification(notification.getJob().getSpecification());
            default: return Optional.empty();
        }
    }

    private Optional<String> getJobCreatedNotificationDestinationFromJobSpecification(JobSpecification jobSpecification) {
        final String destination = jobSpecification.getMailForNotificationAboutVerification();
        if (destination.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(destination);
    }

    private Optional<String> getJobCompletedNotificationDestinationFromJobSpecification(JobSpecification jobSpecification) {
        final String destination = jobSpecification.getMailForNotificationAboutProcessing();
        if (destination.trim().isEmpty() || destination.equals(MISSING_FIELD_VALUE)) {
            // fall back to primary destination
            return getJobCreatedNotificationDestinationFromJobSpecification(jobSpecification);
        }
        return Optional.of(destination);
    }
}
