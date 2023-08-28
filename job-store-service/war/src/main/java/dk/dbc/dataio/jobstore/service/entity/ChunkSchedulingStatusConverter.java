package dk.dbc.dataio.jobstore.service.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus;

/**
 * JPA Converter Class for enum ChunkSchedulingStatus
 */
@Converter(autoApply = true)
public class ChunkSchedulingStatusConverter implements AttributeConverter<ChunkSchedulingStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(ChunkSchedulingStatus chunkSchedulingStatus) {
        return chunkSchedulingStatus.value;
    }

    @Override
    public ChunkSchedulingStatus convertToEntityAttribute(Integer chunkProcessStatus) {
        return Arrays.stream(ChunkSchedulingStatus.values()).filter(s -> s.value.equals(chunkProcessStatus)).findFirst().orElse(null);
    }
}
