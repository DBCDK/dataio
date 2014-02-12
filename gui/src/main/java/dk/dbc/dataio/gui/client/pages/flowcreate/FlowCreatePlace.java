/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.flowcreate;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 *
 * @author slf
 */
public class FlowCreatePlace extends Place {
    private String flowCreateName;

    public FlowCreatePlace() {
        this.flowCreateName = "";
    }

    public FlowCreatePlace(String flowCreateName) {
        this.flowCreateName = flowCreateName;
    }

    public String getFlowCreateName() {
        return flowCreateName;
    }

    @Prefix("CreateFlow")
    public static class Tokenizer implements PlaceTokenizer<FlowCreatePlace> {
        @Override
        public String getToken(FlowCreatePlace place) {
            return place.getFlowCreateName();
        }

        @Override
        public FlowCreatePlace getPlace(String token) {
            return new FlowCreatePlace(token);
        }
    }
    
}
