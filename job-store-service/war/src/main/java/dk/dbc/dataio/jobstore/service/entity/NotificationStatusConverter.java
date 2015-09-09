package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.JobNotification;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class NotificationStatusConverter implements AttributeConverter<JobNotification.Status, Short> {
    @Override
    public Short convertToDatabaseColumn(JobNotification.Status status) {
        return status.getValue();
    }

    @Override
    public JobNotification.Status convertToEntityAttribute(Short value) {
        return JobNotification.Status.getStatusFromValue(value);
    }
}
