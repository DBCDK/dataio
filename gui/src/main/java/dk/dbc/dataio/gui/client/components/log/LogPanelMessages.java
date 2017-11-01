/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    public static String harvestTaskCreated(String dataSetDame, String destination) {
        return logMessageTexts.log_createHarvestTask().replace("$1", dataSetDame).replace("$2", destination);
    }
}
