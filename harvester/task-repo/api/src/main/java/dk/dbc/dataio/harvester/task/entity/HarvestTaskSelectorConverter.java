package dk.dbc.dataio.harvester.task.entity;

import dk.dbc.dataio.harvester.types.HarvestTaskSelector;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class HarvestTaskSelectorConverter implements AttributeConverter<HarvestTaskSelector, String> {
    @Override
    public String convertToDatabaseColumn(HarvestTaskSelector harvestTaskSelector) {
        if (harvestTaskSelector != null) {
            return harvestTaskSelector.toString();
        }
        return null;
    }

    @Override
    public HarvestTaskSelector convertToEntityAttribute(String selectorExpression) throws IllegalArgumentException {
        if (selectorExpression != null) {
            return HarvestTaskSelector.of(selectorExpression);
        }
        return null;
    }
}
