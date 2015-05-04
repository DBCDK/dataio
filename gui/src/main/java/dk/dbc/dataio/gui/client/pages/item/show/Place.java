/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * Place
 */
public class Place extends com.google.gwt.place.shared.Place {
    private String jobId;

    public Place() {
        this("");
    }

    public Place(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    @Prefix("ShowItems")
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return place.jobId;
        }
        @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }

}
