package dk.dbc.dataio.jobstore.distributed.tools;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IntegerArrayToPgIntArrayConverter implements AttributeConverter<Integer[], Object> {
    public IntegerArrayToPgIntArrayConverter() {
    }

    public Object convertToDatabaseColumn(Integer[] array) {
        return array == null ? new PgIntArray(new Integer[0]) : new PgIntArray(array);
    }

    public Integer[] convertToEntityAttribute(Object o) {
        Integer[] array = (Integer[])o;
        return array == null ? new Integer[0] : array;
    }
}
