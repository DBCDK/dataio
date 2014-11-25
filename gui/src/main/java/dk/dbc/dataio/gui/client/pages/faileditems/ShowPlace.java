/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.faileditems;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobState;

/**
 * ShowPlace
 */
public class ShowPlace extends Place {
    private static String combinedUrl;

    public ShowPlace(String url) {
        this.combinedUrl = url;
    }

    public ShowPlace(long jobId, JobState.OperationalState operationalState, ItemCompletionState.State state) {
        this(combineToUrl(jobId, operationalState, state));
    }

    public Long getJobId() {
        try {
            return Long.valueOf(combinedUrl.split(":")[0]);
        } catch (Exception e) {
            return 0L;
        }
    }

    public JobState.OperationalState getOperationalState() {
        try {
            return JobState.OperationalState.valueOf(combinedUrl.split(":")[1]);
        } catch (Exception e) {
            return null;
        }
    }

    public ItemCompletionState.State getStatus() {
        try {
            return ItemCompletionState.State.valueOf(combinedUrl.split(":")[2]);
        } catch (Exception e) {
            return null;
        }
    }

    private static String combineToUrl(Long jobId, JobState.OperationalState operationalState, ItemCompletionState.State state) {
        return jobId + ":" + operationalState + ":" + state;
    }

    @Prefix("FailedItems")
    public static class Tokenizer implements PlaceTokenizer<ShowPlace> {
        @Override
        public String getToken(ShowPlace place) {
            return String.valueOf(combinedUrl);
        }
        @Override
        public ShowPlace getPlace(String token) {
            return new ShowPlace(token);
        }
    }

}
