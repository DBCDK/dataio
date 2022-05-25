package dk.dbc.dataio.jobstore.service.entity;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus;

/**
 * JPA Converter Class for enum ChunkSchedulingStatus
 */
@Converter(autoApply = true)
public class ChunkSchedulingStatusConverter implements AttributeConverter<ChunkSchedulingStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(ChunkSchedulingStatus chunkSchedulingStatus) {
        switch (chunkSchedulingStatus) {
            case READY_FOR_PROCESSING:
                return 1;
            case QUEUED_FOR_PROCESSING:
                return 2;
            case BLOCKED:
                return 3;
            case READY_FOR_DELIVERY:
                return 4;
            case QUEUED_FOR_DELIVERY:
                return 5;
        }
        return null;
    }

    @Override
    public ChunkSchedulingStatus convertToEntityAttribute(Integer chunkProcessStatus) {
        switch (chunkProcessStatus) {
            case 1:
                return ChunkSchedulingStatus.READY_FOR_PROCESSING;
            case 2:
                return ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
            case 3:
                return ChunkSchedulingStatus.BLOCKED;
            case 4:
                return ChunkSchedulingStatus.READY_FOR_DELIVERY;
            case 5:
                return ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
        }
        return null;
    }
}
