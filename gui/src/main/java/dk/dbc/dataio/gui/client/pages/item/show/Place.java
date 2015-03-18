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
    private final String SPLITTER_CHAR = ":";
    private final String COLON_CHARACTER_ENTITY = "&colon;";
    private String combinedUrl;


    public Place(String url) {
        this.combinedUrl = url;
    }

    public Place(String jobId, String submitterName, String sinkName) {
        combinedUrl = encode(jobId) + SPLITTER_CHAR + encode(submitterName) + SPLITTER_CHAR + encode(sinkName);
    }

    public String getJobId() {
        try {
            return decode(combinedUrl.split(SPLITTER_CHAR)[0]);
        } catch (Exception e) {
            return "";
        }
    }

    public String getSubmitterName() {
        try {
            return decode(combinedUrl.split(SPLITTER_CHAR)[1]);
        } catch (Exception e) {
            return "";
        }
    }

    public String getSinkName() {
        try {
            return decode(combinedUrl.split(SPLITTER_CHAR)[2]);
        } catch (Exception e) {
            return "";
        }
    }

    @Prefix("ShowItems")
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return place.combinedUrl;
        }
        @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }

    private String encode(String value) {
        return value.replace(SPLITTER_CHAR, COLON_CHARACTER_ENTITY);
    }

    private String decode(String value) {
        return value.replace(COLON_CHARACTER_ENTITY, SPLITTER_CHAR);
    }
}
