package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemListQuery;
import dk.dbc.dataio.jobstore.service.util.JobstoreDB;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@Stateless
public class JobExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobExporter.class);
    private static final int MAX_NUMBER_OF_ITEMS_PER_QUERY = 1000;

    @Inject @JobstoreDB
    EntityManager entityManager;

    public ByteArrayOutputStream exportFailedItems(int jobId, State.Phase fromPhase, ChunkItem.Type asType, Charset encodedAs) throws JobStoreException {
        LOGGER.info("Exporting failed items for job {} from phase {} as {} encoded as {}", jobId, fromPhase, asType, encodedAs);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int offset = 0;
        int numberOfItemsFound;
        do {
            final ItemListCriteria itemListCriteria = new ItemListCriteria()
                    .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId))
                    .and(new ListFilter<>(phaseToCriteriaField(fromPhase)))
                    .orderBy(new ListOrderBy<>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC))
                    .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC))
                    .offset(offset)
                    .limit(MAX_NUMBER_OF_ITEMS_PER_QUERY);

            final ItemListQuery itemListQuery = new ItemListQuery(entityManager);
            final List<ItemEntity> items = itemListQuery.execute(itemListCriteria);

            numberOfItemsFound = items.size();
            if (numberOfItemsFound > 0) {
                offset += numberOfItemsFound;
                for (ItemEntity item : items) {
                    try {
                        buffer.write(exportFailedItem(item, fromPhase, asType, encodedAs));
                    } catch (IOException e) {
                        final String message = String.format("Exception caught during export of failed items for job %d chunk %d item %d",
                                item.getKey().getJobId(), item.getKey().getChunkId(), item.getKey().getId());
                        throw new JobStoreException(message, e);
                    }
                }
            }
        } while (numberOfItemsFound == MAX_NUMBER_OF_ITEMS_PER_QUERY);

        return buffer;
    }

    byte[] exportFailedItem(ItemEntity item, State.Phase fromPhase, ChunkItem.Type asType, Charset encodedAs) {
        LOGGER.trace("Called with item={}, fromPhase={}, asType={}, encodedAs={}", item, fromPhase, asType, encodedAs);
        return null;
    }

    ItemListCriteria.Field phaseToCriteriaField(State.Phase phase) {
        switch (phase) {
            case PARTITIONING: return ItemListCriteria.Field.PARTITIONING_FAILED;
            case PROCESSING:   return ItemListCriteria.Field.PROCESSING_FAILED;
            case DELIVERING:   return ItemListCriteria.Field.DELIVERY_FAILED;
            default: throw new IllegalStateException("Unknown phase " + phase);
        }
    }
}
