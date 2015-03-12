package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;

import java.util.ArrayList;
import java.util.List;

/*
 * This class maps returned job info snapshots (from an item search) to an item model.
 * the methods contain different logic for the field: LifeCycle.
 *
 * Depending on the search type, the value is referring to:
 *
 * FAILED/IGNORED   -> in which phase (PARTITIONING, PROCESSING, DELIVERING) was an item failed/ignored
 * ALL              -> the current phase of the item (PARTITIONING, PROCESSING, DELIVERING, DONE)
 */
public final class ItemModelMapper {

    /**
     * Maps a list of item info snapshots to a list of item models for an item search locating FAILED items
     * belonging to a specific job
     *
     * @param itemInfoSnapshots item info snapshots to map
     * @return list of item model containing the mapped values
     */
    public static List<ItemModel> toFailedItemsModel(List<ItemInfoSnapshot> itemInfoSnapshots) {
        List<ItemModel> itemInfoSnapshotModels = new ArrayList<ItemModel>(itemInfoSnapshots.size());
        for (ItemInfoSnapshot itemInfoSnapshot : itemInfoSnapshots) {
            itemInfoSnapshotModels.add(toFailedItemsModel(itemInfoSnapshot));
        }
        return itemInfoSnapshotModels;
    }

    /**
     * Maps a list of item info snapshots to a list of item models for an item search locating ALL items
     * belonging to a specific job
     *
     * @param itemInfoSnapshots item info snapshots to map
     * @return list of item model containing the mapped values
     */
    public static List<ItemModel> toAllItemsModel(List<ItemInfoSnapshot> itemInfoSnapshots) {
        List<ItemModel> itemInfoSnapshotModels = new ArrayList<ItemModel>(itemInfoSnapshots.size());
        for (ItemInfoSnapshot itemInfoSnapshot : itemInfoSnapshots) {
            itemInfoSnapshotModels.add(toAllItemsModel(itemInfoSnapshot));
        }
        return itemInfoSnapshotModels;
    }

    /*
     * Private methods
     */

    /**
     * Maps an item info snapshot to an item model for a search locating all FAILED items for a specific job
     *
     * @param itemInfoSnapshot item info snapshot to map
     * @return item model containing the mapped values
     */
    private static ItemModel toFailedItemsModel(ItemInfoSnapshot itemInfoSnapshot) {
        return new ItemModel(
                Long.valueOf(itemInfoSnapshot.getItemNumber()).toString(),
                Long.valueOf(itemInfoSnapshot.getItemId()).toString(),
                Long.valueOf(itemInfoSnapshot.getChunkId()).toString(),
                Long.valueOf(itemInfoSnapshot.getJobId()).toString(),
                searchFailed(itemInfoSnapshot.getState()));
    }

    /**
     * Maps an item info snapshot to an item model for a search locating ALL items for a specific job
     *
     * @param itemInfoSnapshot item info snapshot to map
     * @return item model containing the mapped values
     */
    private static ItemModel toAllItemsModel(ItemInfoSnapshot itemInfoSnapshot) {
        return new ItemModel(
                Long.valueOf(itemInfoSnapshot.getItemNumber()).toString(),
                Long.valueOf(itemInfoSnapshot.getItemId()).toString(),
                Long.valueOf(itemInfoSnapshot.getChunkId()).toString(),
                Long.valueOf(itemInfoSnapshot.getJobId()).toString(),
                searchAll(itemInfoSnapshot.getState()));
    }

    /**
     * This method determines the return value based on the phase in which the item failed
     *
     * @param state containing information regarding the status of the item (success, failed, ignored)
     * @return the life cycle (which phase has the item has failed in).
     */
    private static ItemModel.LifeCycle searchFailed(State state) {
        ItemModel.LifeCycle lifeCycle = ItemModel.LifeCycle.PARTITIONING; //Default value
        if (state.getPhase(State.Phase.PROCESSING).getFailed() == 1) {
            lifeCycle = ItemModel.LifeCycle.PROCESSING;
        } else if (state.getPhase(State.Phase.DELIVERING).getFailed() == 1) {
            lifeCycle = ItemModel.LifeCycle.DELIVERING;
        }
        return lifeCycle;
    }

    /**
     * This method determines the return value based on the current phase of the item
     *
     * @param state containing information regarding the status of the item (success, failed, ignored)
     * @return the life cycle (which phase is the item is currently in).
     */
    private static ItemModel.LifeCycle searchAll(State state) {
        ItemModel.LifeCycle lifeCycle = ItemModel.LifeCycle.PARTITIONING; //Default value;
        if (state.allPhasesAreDone()) {
            lifeCycle = ItemModel.LifeCycle.DONE;
        } else {
            if (!state.phaseIsDone(State.Phase.PROCESSING)) {
                lifeCycle = ItemModel.LifeCycle.PROCESSING;
            } else if (!state.phaseIsDone(State.Phase.DELIVERING)) {
                lifeCycle = ItemModel.LifeCycle.DELIVERING;
            }
        }
        return lifeCycle;
    }
}
