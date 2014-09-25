/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.faileditems;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * ShowPlace
 */
public class ShowPlace extends Place {
    private long jobId;

    public ShowPlace(String url) {
        this.jobId = Long.parseLong(url);
    }

    public ShowPlace(long jobId) {
        this.jobId = jobId;
    }

    public long getjobId() {
        return jobId;
    }

    @Prefix("FailedItems")
    public static class Tokenizer implements PlaceTokenizer<ShowPlace> {
        @Override
        public String getToken(ShowPlace place) {
            return String.valueOf(place.getjobId());
        }
        @Override
        public ShowPlace getPlace(String token) {
            return new ShowPlace(token);
        }
    }

}
