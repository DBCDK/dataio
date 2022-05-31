package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.State;

import java.util.ArrayList;
import java.util.List;

/*
 * This class maps returned job info snapshots (from an item search) to an item model.
 *
 * Depending on the search type (FAILED, IGNORED, ALL), LifeCycle is referring to:
 *
 * FAILED/IGNORED   -> In which phase (PARTITIONING, PROCESSING, DELIVERING) was an item failed/ignored
 * ALL              -> The phase which the item is currently in (PARTITIONING, PROCESSING, DELIVERING, DONE)
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
        List<ItemModel> itemInfoSnapshotModels = new ArrayList<>(itemInfoSnapshots.size());
        for (ItemInfoSnapshot itemInfoSnapshot : itemInfoSnapshots) {
            itemInfoSnapshotModels.add(toFailedItemsModel(itemInfoSnapshot));
        }
        return itemInfoSnapshotModels;
    }

    /**
     * Maps a list of item info snapshots to a list of item models for an item search locating IGNORED items
     * belonging to a specific job
     *
     * @param itemInfoSnapshots item info snapshots to map
     * @return list of item model containing the mapped values
     */
    public static List<ItemModel> toIgnoredItemsModel(List<ItemInfoSnapshot> itemInfoSnapshots) {
        List<ItemModel> itemInfoSnapshotModels = new ArrayList<>(itemInfoSnapshots.size());
        for (ItemInfoSnapshot itemInfoSnapshot : itemInfoSnapshots) {
            itemInfoSnapshotModels.add(toIgnoredItemsModel(itemInfoSnapshot));
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
        List<ItemModel> itemInfoSnapshotModels = new ArrayList<>(itemInfoSnapshots.size());
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
    public static ItemModel toFailedItemsModel(ItemInfoSnapshot itemInfoSnapshot) {
        return new ItemModel(
                Long.valueOf(itemInfoSnapshot.getItemNumber()).toString(),
                Long.valueOf(itemInfoSnapshot.getItemId()).toString(),
                Long.valueOf(itemInfoSnapshot.getChunkId()).toString(),
                Long.valueOf(itemInfoSnapshot.getJobId()).toString(),
                getRecordId(itemInfoSnapshot.getRecordInfo()),
                searchFailed(itemInfoSnapshot.getState()),
                toDiagnosticModels(itemInfoSnapshot.getState().getDiagnostics()),
                hasFatalDiagnostic(itemInfoSnapshot.getState().getDiagnostics()),
                WorkflowNoteModelMapper.toWorkflowNoteModel(itemInfoSnapshot.getWorkflowNote()),
                itemInfoSnapshot.getTrackingId());
    }

    /**
     * Maps a list of Diagnostics to a list of Diagnostic models
     *
     * @param diagnostics the list of diagnostics to map
     * @return list of diagnostic models
     */
    private static List<DiagnosticModel> toDiagnosticModels(List<Diagnostic> diagnostics) {
        List<DiagnosticModel> diagnosticModels = new ArrayList<>(diagnostics.size());
        for (Diagnostic diagnostic : diagnostics) {
            diagnosticModels.add(new DiagnosticModel(diagnostic.getLevel().name(), diagnostic.getMessage(), diagnostic.getStacktrace()));
        }
        return diagnosticModels;
    }

    /**
     * Determines if an item contains any diagnostic with level FATAL
     *
     * @param diagnostics the list of diagnostics
     * @return true if a diagnostic with level FATAL is located, otherwise false
     */
    private static boolean hasFatalDiagnostic(List<Diagnostic> diagnostics) {
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getLevel() == Diagnostic.Level.FATAL) {
                return true;
            }
        }
        return false;
    }

    /**
     * Maps an item info snapshot to an item model for a search locating all IGNORED items for a specific job
     *
     * @param itemInfoSnapshot item info snapshot to map
     * @return item model containing the mapped values
     */
    private static ItemModel toIgnoredItemsModel(ItemInfoSnapshot itemInfoSnapshot) {
        return new ItemModel(
                Long.valueOf(itemInfoSnapshot.getItemNumber()).toString(),
                Long.valueOf(itemInfoSnapshot.getItemId()).toString(),
                Long.valueOf(itemInfoSnapshot.getChunkId()).toString(),
                Long.valueOf(itemInfoSnapshot.getJobId()).toString(),
                getRecordId(itemInfoSnapshot.getRecordInfo()),
                searchIgnored(itemInfoSnapshot.getState()),
                toDiagnosticModels(itemInfoSnapshot.getState().getDiagnostics()),
                hasFatalDiagnostic(itemInfoSnapshot.getState().getDiagnostics()),
                WorkflowNoteModelMapper.toWorkflowNoteModel(itemInfoSnapshot.getWorkflowNote()),
                itemInfoSnapshot.getTrackingId());
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
                getRecordId(itemInfoSnapshot.getRecordInfo()),
                searchAll(itemInfoSnapshot.getState()),
                toDiagnosticModels(itemInfoSnapshot.getState().getDiagnostics()),
                hasFatalDiagnostic(itemInfoSnapshot.getState().getDiagnostics()),
                WorkflowNoteModelMapper.toWorkflowNoteModel(itemInfoSnapshot.getWorkflowNote()),
                itemInfoSnapshot.getTrackingId());
    }

    /**
     * This method determines the return value based on the phase in which the item failed
     *
     * @param state containing information regarding the status of the item (success, failed, ignored)
     * @return the life cycle (in which phase has the item failed).
     */
    private static ItemModel.LifeCycle searchFailed(State state) {
        ItemModel.LifeCycle lifeCycle = ItemModel.LifeCycle.PARTITIONING_FAILED; //Default value
        if (state.getPhase(State.Phase.PROCESSING).getFailed() == 1) {
            lifeCycle = ItemModel.LifeCycle.PROCESSING_FAILED;
        } else if (state.getPhase(State.Phase.DELIVERING).getFailed() == 1) {
            lifeCycle = ItemModel.LifeCycle.DELIVERING_FAILED;
        }
        return lifeCycle;
    }

    /**
     * This method determines the return value based on the phase in which the item was firstly ignored
     *
     * @param state containing information regarding the status of the item (success, failed, ignored)
     * @return the life cycle (in which phase has the item firstly been ignored).
     */
    private static ItemModel.LifeCycle searchIgnored(State state) {
        ItemModel.LifeCycle lifeCycle = ItemModel.LifeCycle.PARTITIONING_FAILED; //Default value
        if (state.getPhase(State.Phase.PROCESSING).getIgnored() == 1) {
            lifeCycle = ItemModel.LifeCycle.PROCESSING_IGNORED;
        } else if (state.getPhase(State.Phase.DELIVERING).getIgnored() == 1) {
            lifeCycle = ItemModel.LifeCycle.DELIVERING_IGNORED;
        }
        return lifeCycle;
    }


    /**
     * This method determines the return value based on the current phase of the item
     *
     * @param state containing information regarding the status of the item (success, failed, ignored)
     * @return the life cycle (in which phase is the item is currently).
     */
    private static ItemModel.LifeCycle searchAll(State state) {
        ItemModel.LifeCycle lifeCycle = ItemModel.LifeCycle.PARTITIONING; //Default value;
        if (state.allPhasesAreDone()) {
            if (state.getPhase(State.Phase.PARTITIONING).getFailed() > 0) {
                lifeCycle = ItemModel.LifeCycle.PARTITIONING_FAILED;
            } else if (state.getPhase(State.Phase.PARTITIONING).getIgnored() > 0) {
                lifeCycle = ItemModel.LifeCycle.PARTITIONING_IGNORED;
            } else if (state.getPhase(State.Phase.PROCESSING).getFailed() > 0) {
                lifeCycle = ItemModel.LifeCycle.PROCESSING_FAILED;
            } else if (state.getPhase(State.Phase.PROCESSING).getIgnored() > 0) {
                lifeCycle = ItemModel.LifeCycle.PROCESSING_IGNORED;
            } else if (state.getPhase(State.Phase.DELIVERING).getFailed() > 0) {
                lifeCycle = ItemModel.LifeCycle.DELIVERING_FAILED;
            } else if (state.getPhase(State.Phase.DELIVERING).getIgnored() > 0) {
                lifeCycle = ItemModel.LifeCycle.DELIVERING_IGNORED;
            } else {
                lifeCycle = ItemModel.LifeCycle.DONE;
            }
        } else {
            if (!state.phaseIsDone(State.Phase.PROCESSING)) {
                lifeCycle = ItemModel.LifeCycle.PROCESSING;
            } else if (!state.phaseIsDone(State.Phase.DELIVERING)) {
                lifeCycle = ItemModel.LifeCycle.DELIVERING;
            }
        }
        return lifeCycle;
    }

    private static String getRecordId(RecordInfo recordInfo) {
        return recordInfo == null ? "" : recordInfo.getId();
    }
}
