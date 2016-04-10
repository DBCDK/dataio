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
            case ReadyToProcess:  return 1;
            case QueuedToProcess: return 2;
            case Blocked:         return 3;
            case ReadyDelevering: return 4;
            case QueuedToSink:    return 5;
            default:
        }
        return null;
    }

    @Override
    public ChunkProcessStatus convertToEntityAttribute(Integer chunkProcessStatus) {
        switch ( chunkProcessStatus ) {
            case 1: return ReadyToProcess;
            case 2: return QueuedToProcess;
            case 3: return Blocked;
            case 4: return ReadyDelevering;
            case 5: return QueuedToSink;
            default:
        }
        return null;
    }


}
