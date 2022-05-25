package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.Notification;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class NotificationStatusConverter implements AttributeConverter<Notification.Status, Short> {
    @Override
    public Short convertToDatabaseColumn(Notification.Status status) {
        return status.getValue();
    }

    @Override
    public Notification.Status convertToEntityAttribute(Short value) {
        return Notification.Status.of(value);
    }
}
