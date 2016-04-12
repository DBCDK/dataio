package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import static org.eclipse.persistence.expressions.ExpressionOperator.Sin;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by ja7 on 11-04-16.
 */
@Stateless
public class NewJobSchedulerBean {

    @Inject
    @JobstoreDB
    EntityManager entityManager;
    /**
     * ScheduleChunk

     * Passes given chunk collision detection element and sink on to the
     * sequence analyser and notifies pipeline of next available workload (if any)
     * @param chunk next chunk element to enter into sequence analysis
     * @param sink sink associated with chunk
     * @throws NullPointerException if given any null-valued argument
     * @throws JobStoreException if unable to setup monitoring
     */
    public void scheduleChunk(ChunkEntity chunk, Sink sink) throws  JobStoreException{
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        InvariantUtil.checkNotNullOrThrow(sink, "sink");
    }


    /**
     * Retuens List of Chunks To wait for.
     * @param matchKeys
     * @return
     */
    List<DependencyTrackingEntity.Key> findChunksToWaitFor(int sinkId, Set<String> matchKeys ) {
        if( matchKeys.size() == 0 ) return new ArrayList<>();

        Query query=entityManager.createNativeQuery( buildFindChunksToWaitForQuery( sinkId, matchKeys ), "JobIdChunkIdResult");
        List<DependencyTrackingEntity.Key> result=query.getResultList();
        return result;
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
