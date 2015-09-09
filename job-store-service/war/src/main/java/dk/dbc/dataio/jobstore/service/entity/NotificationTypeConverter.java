package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.JobNotification;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class NotificationTypeConverter implements AttributeConverter<JobNotification.Type, Short> {
    @Override
    public Short convertToDatabaseColumn(JobNotification.Type type) {
        return type.getValue();
    }

    @Override
    public JobNotification.Type convertToEntityAttribute(Short value) {
        return JobNotification.Type.getTypeFromValue(value);
    }
}
