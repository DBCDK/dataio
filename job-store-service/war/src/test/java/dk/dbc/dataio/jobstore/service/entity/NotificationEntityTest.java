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

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.test.types.JobNotificationBuilder;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NotificationEntityTest {
    @Test
    public void toJobNotification() {
        final JobNotification expectedJobNotification = new JobNotificationBuilder()
                .build();
        final JobEntity jobEntity = new JobEntity(expectedJobNotification.getJobId());
        final NotificationEntity notificationEntity = new NotificationEntity(
                expectedJobNotification.getId(),
                expectedJobNotification.getTimeOfCreation(),
                expectedJobNotification.getTimeOfLastModification()
        );
        notificationEntity.setType(expectedJobNotification.getType());
        notificationEntity.setStatus(expectedJobNotification.getStatus());
        notificationEntity.setStatusMessage(expectedJobNotification.getStatusMessage());
        notificationEntity.setDestination(expectedJobNotification.getDestination());
        notificationEntity.setContent(expectedJobNotification.getContent());
        notificationEntity.setJob(jobEntity);
        assertThat(notificationEntity.toJobNotification(), is(expectedJobNotification));
    }

    @Test
    public void toNotification() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final InvalidTransfileNotificationContext invalidTransfileNotificationContext =
                new InvalidTransfileNotificationContext("filename", "content", "invalid");

        final JobNotification jobNotification = new JobNotificationBuilder().build();
        final JobEntity jobEntity = new JobEntity(jobNotification.getJobId());
        final NotificationEntity notificationEntity = new NotificationEntity(
                jobNotification.getId(),
                jobNotification.getTimeOfCreation(),
                jobNotification.getTimeOfLastModification()
        );
        notificationEntity.setType(jobNotification.getType());
        notificationEntity.setStatus(jobNotification.getStatus());
        notificationEntity.setStatusMessage(jobNotification.getStatusMessage());
        notificationEntity.setDestination(jobNotification.getDestination());
        notificationEntity.setContent(jobNotification.getContent());
        notificationEntity.setJob(jobEntity);
        notificationEntity.setContext(jsonbContext.marshall(invalidTransfileNotificationContext));

        final Notification notification = notificationEntity.toNotification();
        assertThat("id", notification.getId(), is(notificationEntity.getId()));
        assertThat("timeOfCreation", notification.getTimeOfCreation(), is(notificationEntity.getTimeOfCreation()));
        assertThat("timeOfLastModification", notification.getTimeOfLastModification(), is(notificationEntity.getTimeOfLastModification()));
        assertThat("type", notification.getType(), is(notificationEntity.getType().toNotificationType()));
        assertThat("status", notification.getStatus(), is(notificationEntity.getStatus().toNotificationStatus()));
        assertThat("statusMessage", notification.getStatusMessage(), is(notificationEntity.getStatusMessage()));
        assertThat("content", notification.getContent(), is(notificationEntity.getContent()));
        assertThat("jobId", notification.getJobId(), is(notificationEntity.getJob().getId()));
        assertThat("context", notification.getContext(), is(invalidTransfileNotificationContext));
    }
}