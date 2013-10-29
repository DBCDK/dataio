/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.places;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * SinkCreatePlace
 * 
 * @author slf
 */
public class SinkCreatePlace extends Place {
    private String sinkCreateName;

    public SinkCreatePlace() {
        this.sinkCreateName = "";
    }

    public SinkCreatePlace(String sinkCreateName) {
        this.sinkCreateName = sinkCreateName;
    }

    public String getSinkCreateName() {
        return sinkCreateName;
    }

    @Prefix("CreateSink")
    public static class Tokenizer implements PlaceTokenizer<dk.dbc.dataio.gui.client.places.SinkCreatePlace> {
        @Override
        public String getToken(dk.dbc.dataio.gui.client.places.SinkCreatePlace place) {
            return place.getSinkCreateName();
        }

        @Override
        public dk.dbc.dataio.gui.client.places.SinkCreatePlace getPlace(String token) {
            return new dk.dbc.dataio.gui.client.places.SinkCreatePlace(token);
        }
    }
    
}
