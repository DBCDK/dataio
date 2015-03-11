package dk.dbc.dataio.gui.server.ModelMappers.Criterias;

import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;

public class ItemListCriteriaModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private ItemListCriteriaModelMapper() {}

    /**
     * This method defines the search criteria for locating failed items within a job
     *
     * @param model failed item list criteria model, containing the values needed to perform the search
     * @return ItemListCriteria with added filtering parameters
     */
    public static ItemListCriteria toFailedItemListCriteria(ItemListCriteriaModel model) {
        ListFilter jobIdEqualsCondition = new ListFilter<ItemListCriteria.Field>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, Long.valueOf(model.getJobId()).intValue());
        ListFilter itemStatus = new ListFilter<ItemListCriteria.Field>(ItemListCriteria.Field.STATE_FAILED);
        ListOrderBy ascendingChunkId = new ListOrderBy<ItemListCriteria.Field>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC);
        ListOrderBy ascendingItemId = new ListOrderBy<ItemListCriteria.Field>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC);
        return new ItemListCriteria().where(jobIdEqualsCondition).and(itemStatus).orderBy(ascendingChunkId).orderBy(ascendingItemId);
    }

    /**
     * This method defines the seach criteria for locating all items items contained within a job
     *
     * @param model item list criteria model, containing the values needed to perform the search
     * @return ItemListCriteria with added filtering parameters
     */
    public static ItemListCriteria toItemListCriteriaAll(ItemListCriteriaModel model) {
        ListFilter jobIdEqualsCondition = new ListFilter<ItemListCriteria.Field>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, Long.valueOf(model.getJobId()).intValue());
        ListOrderBy ascendingChunkId = new ListOrderBy<ItemListCriteria.Field>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC);
        ListOrderBy ascendingItemId = new ListOrderBy<ItemListCriteria.Field>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC);
        return new ItemListCriteria().where(jobIdEqualsCondition).orderBy(ascendingChunkId).orderBy(ascendingItemId);
    }
}
