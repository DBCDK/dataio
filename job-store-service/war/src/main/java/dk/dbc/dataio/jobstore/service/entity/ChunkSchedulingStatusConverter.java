package dk.dbc.dataio.jobstore.service.entity;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.BLOCKED;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_PROCESSING;

/**
 * JPA Converter Class for enum ChunkProcessStatus
 *
 */
@Converter(autoApply = true)
public class ChunkSchedulingStatusConverter implements AttributeConverter<ChunkSchedulingStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ChunkSchedulingStatus chunkSchedulingStatus) {
        switch (chunkSchedulingStatus) {
            case READY_FOR_PROCESSING:     return 1;
            case QUEUED_FOR_PROCESSING:    return 2;
            case BLOCKED:              return 3;
            case READY_FOR_DELIVERY:     return 4;
            case QUEUED_FOR_DELIVERY: return 5;
            default:
        }
        return null;
    }

    @Override
    public ChunkSchedulingStatus convertToEntityAttribute(Integer chunkProcessStatus) {
        switch ( chunkProcessStatus ) {
            case 1: return READY_FOR_PROCESSING;
            case 2: return QUEUED_FOR_PROCESSING;
            case 3: return BLOCKED;
            case 4: return READY_FOR_DELIVERY;
            case 5: return QUEUED_FOR_DELIVERY;
            default:
        }
        return null;
    }


}
