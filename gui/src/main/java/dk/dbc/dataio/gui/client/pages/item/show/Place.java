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

    public Place(String jobId, String submitterNumber, String sinkName, String itemCounter, String failedItemCounter, String ignoredItemCounter) {
        combinedUrl = encode(jobId) + SPLITTER_CHAR
                + encode(submitterNumber) + SPLITTER_CHAR
                + encode(sinkName) + SPLITTER_CHAR
                + encode(itemCounter) + SPLITTER_CHAR
                + encode(failedItemCounter) + SPLITTER_CHAR
                + encode(ignoredItemCounter);
    }

    public String getJobId() {
        try {
            return decode(combinedUrl.split(SPLITTER_CHAR)[0]);
        } catch (Exception e) {
            return "";
        }
    }

    public String getSubmitterNumber() {
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

    public String getItemCounter() {
        try {
            return decode(combinedUrl.split(SPLITTER_CHAR)[3]);
        } catch (Exception e) {
            return "";
        }
    }

    public String getFailedItemCounter() {
        try {
            return decode(combinedUrl.split(SPLITTER_CHAR)[4]);
        } catch (Exception e) {
            return "";
        }
    }

    public String getIgnoredItemCounter() {
        try {
            return decode(combinedUrl.split(SPLITTER_CHAR)[5]);
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
