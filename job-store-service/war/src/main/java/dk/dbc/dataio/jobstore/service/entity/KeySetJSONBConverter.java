package dk.dbc.dataio.jobstore.service.entity;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.partioner.entity.ConverterJSONBContext;
import dk.dbc.dataio.jobstore.service.dependencytracking.TrackingKey;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

@Converter
public class KeySetJSONBConverter implements AttributeConverter<Set<TrackingKey>, PGobject> {

    final CollectionType JSONSetKeyType = ConverterJSONBContext.getInstance().getTypeFactory().constructCollectionType(Set.class, TrackingKey.class);

    @Override
    public PGobject convertToDatabaseColumn(Set<TrackingKey> sequenceAnalysisData) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(ConverterJSONBContext.getInstance().marshall(sequenceAnalysisData));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public Set<TrackingKey> convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            if (pgObject == null) return new LinkedHashSet<>();
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), JSONSetKeyType);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
