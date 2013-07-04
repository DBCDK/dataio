/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.places;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

/**
 *
 * @author slf
 */
public class FlowEditPlace extends Place {
    private String flowEditName;

    public FlowEditPlace(String token) {
        this.flowEditName = token;
    }

    public String getFlowEditName() {
        return flowEditName;
    }

    public static class Tokenizer implements PlaceTokenizer<FlowEditPlace> {
        @Override
        public String getToken(FlowEditPlace place) {
            return place.getFlowEditName();
        }

        @Override
        public FlowEditPlace getPlace(String token) {
            return new FlowEditPlace(token);
        }
    }
    
}
