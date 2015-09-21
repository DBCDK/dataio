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
import dk.dbc.dataio.jobstore.types.JobNotification;
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
}