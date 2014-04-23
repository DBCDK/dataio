/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.sinkcreateedit;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.commons.types.Sink;


/**
 * SinkEditPlace
 * 
 * @author slf
 */
public class SinkEditPlace extends Place {
    private Long sinkId;

    public SinkEditPlace(String url) {
        this.sinkId = Long.valueOf(url);
    }

    public SinkEditPlace(Sink sink) {
        this.sinkId = sink.getId();
    }

    public Long getSinkId() {
        return sinkId;
    }

    @Prefix("EditSink")
    public static class Tokenizer implements PlaceTokenizer<SinkEditPlace> {
        @Override
        public String getToken(SinkEditPlace place) {
            return String.valueOf(place.getSinkId());
        }
        @Override
        public SinkEditPlace getPlace(String token) {
            return new SinkEditPlace(token);
        }
    }

}
