package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.Notification;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class NotificationTypeConverter implements AttributeConverter<Notification.Type, Short> {
    @Override
    public Short convertToDatabaseColumn(Notification.Type type) {
        return type.getValue();
    }

    @Override
    public Notification.Type convertToEntityAttribute(Short value) {
        return Notification.Type.of(value);
    }
}
