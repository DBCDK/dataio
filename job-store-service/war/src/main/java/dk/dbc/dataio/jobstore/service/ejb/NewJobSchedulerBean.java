package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ConverterJSONBContext;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jsonb.JSONBException;
import static org.eclipse.persistence.expressions.ExpressionOperator.Sin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import javax.inject.Inject;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by ja7 on 11-04-16.
 *
 * Handle Chunk Scheduling.
 */
@Stateless
public class NewJobSchedulerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewJobSchedulerBean.class);

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducerBean;
    @EJB
    private PgJobStoreRepository jobStoreRepository;


    /**
     * ScheduleChunk

     * Passes given chunk collision detection element and sink on to the
     * sequence analyser and notifies pipeline of next available workload (if any)
     * @param chunk next chunk element to enter into sequence analysis
     * @param sink sink associated with chunk
     * @throws NullPointerException if given any null-valued argument
     * @throws JobStoreException if unable to setup monitoring
     */
    @Stopwatch
    public void scheduleChunk(ChunkEntity chunk, Sink sink) throws  JobStoreException{
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        InvariantUtil.checkNotNullOrThrow(sink, "sink");

        persistDependencyEntity(chunk, sink);
        submitIfPosibleForProcessing( chunk, sink );

    }

    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void persistDependencyEntity(ChunkEntity chunk, Sink sink) {
        int sinkId=(int)sink.getId();

        DependencyTrackingEntity e=new DependencyTrackingEntity( chunk, sinkId );
        e.setWaitingOn( findChunksToWaitFor( sinkId, e.getMatchKeys()));
        entityManager.persist( e );
    }

    @Asynchronous
    @Stopwatch
    void submitIfPosibleForProcessing(ChunkEntity chunk, Sink sink) {
        try {
            jobProcessorMessageProducerBean.send( getChunkFromChunkEntity(chunk));
        } catch (JobStoreException e) {
            LOGGER.error("Unable to send processing notification for {}", chunk.getKey().toString(), e);
        }
    }

    private Chunk getChunkFromChunkEntity(ChunkEntity chunk) {
        ChunkEntity.Key chunkKey=chunk.getKey();
        return jobStoreRepository.getChunk( Chunk.Type.PARTITIONED, chunkKey.getJobId(), chunkKey.getId() );
    }


    /**
     * Retuens List of Chunks To wait for.
     * @param matchKeys
     * @return
     */
    List<DependencyTrackingEntity.Key> findChunksToWaitFor(int sinkId, Set<String> matchKeys ) {
        if( matchKeys.isEmpty() ) return new ArrayList<>();

        Query query=entityManager.createNativeQuery( buildFindChunksToWaitForQuery( sinkId, matchKeys ), "JobIdChunkIdResult");
        List<DependencyTrackingEntity.Key> result=query.getResultList();
        return result;
    };


    List<DependencyTrackingEntity.Key> findChunksWaitingForMe( DependencyTrackingEntity.Key key ) throws JSONBException {
        String keyAsJson= ConverterJSONBContext.getInstance().marshall(key);
        Query query=entityManager.createNativeQuery("select jobid, chunkid from dependencyTracking where waitingon @> '["+ keyAsJson +"]'" , "JobIdChunkIdResult");
        return query.getResultList();
    };

    /**
     * @param sinkId  Sink Id to find
     * @param matchKeys  Set of keys any chunk with any key is returned
     * @return  NativeQuery for find chunks to wait for using @>
     */
    String buildFindChunksToWaitForQuery( int sinkId, Set<String> matchKeys ) {
        StringBuilder builder= new StringBuilder(1000);
        builder.append("select jobid, chunkid from dependencyTracking where sinkId=");
        builder.append( sinkId );
        builder.append(" and ( ");


        Boolean first=true;
        for( String key: matchKeys ) {
            if( ! first ) builder.append(" or ");
            builder.append("matchkeys @> '[\"");
            builder.append(key);
            builder.append("\"]'");
            first=false;
        }
        builder.append(" )");
        return builder.toString();
    }
}
