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