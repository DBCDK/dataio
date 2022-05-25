package dk.dbc.dataio.gui.client.components.log;

import com.google.gwt.core.client.GWT;
import dk.dbc.dataio.gui.client.exceptions.texts.LogMessageTexts;

public class LogPanelMessages {
    private static LogMessageTexts logMessageTexts = GWT.create(LogMessageTexts.class);

    /* Rerun job messages */
    public static String rerunCanceledTickle(String jobId) {
        return logMessageTexts.log_rerunCanceledTickle().replace("$1", jobId);
    }

    public static String rerunCanceledFatalDiagnostic(String jobId) {
        return logMessageTexts.log_rerunCanceledFatalDiagnostic().replace("$1", jobId);
    }

    public static String rerunCanceledNoFailed(String jobId) {
        return logMessageTexts.log_rerunCanceledNoFailed().replace("$1", jobId);
    }

    public static String rerunFromFileStore(String newJobId, String oldJobId) {
        return logMessageTexts.log_rerunFileStore().replace("$1", newJobId).replace("$2", oldJobId);
    }

    public static String rerunFromJobStore(boolean isFailedItemsOnly, String oldJobId) {
        final String msg = isFailedItemsOnly ? logMessageTexts.log_failedItems() : logMessageTexts.log_allItems();
        return logMessageTexts.log_rerunJobStore().replace("$1", msg).replace("$2", oldJobId);
    }

    public static String harvestTaskCreated(String dataSetName, String destination) {
        return logMessageTexts.log_createHarvestTask().replace("$1", dataSetName).replace("$2", destination);
    }
}
