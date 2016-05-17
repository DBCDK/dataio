package dk.dbc.dataio.jobstore.service.entity;

import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkProcessStatus;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkProcessStatus.*;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * JPA Converter Class for enum ChunkProcessStatus
 *
 */
@Converter(autoApply = true)
public class ChunkProcessStatusConverter implements AttributeConverter<ChunkProcessStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ChunkProcessStatus chunkProcessStatus) {
        switch ( chunkProcessStatus ) {
            case READY_TO_PROCESS:     return 1;
            case QUEUED_TO_PROCESS:    return 2;
            case BLOCKED:              return 3;
            case READY_TO_DELIVER:     return 4;
            case QUEUED_TO_DELIVERY: return 5;
            default:
        }
        return null;
    }

    @Override
    public ChunkProcessStatus convertToEntityAttribute(Integer chunkProcessStatus) {
        switch ( chunkProcessStatus ) {
            case 1: return READY_TO_PROCESS;
            case 2: return QUEUED_TO_PROCESS;
            case 3: return BLOCKED;
            case 4: return READY_TO_DELIVER;
            case 5: return QUEUED_TO_DELIVERY;
            default:
        }
        return null;
    }


}
