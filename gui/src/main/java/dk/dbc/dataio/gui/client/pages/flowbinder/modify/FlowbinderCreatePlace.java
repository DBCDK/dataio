/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 *
 * @author slf
 */
public class FlowbinderCreatePlace extends Place {
    private String flowbinderCreateName;

    public FlowbinderCreatePlace() {
        this.flowbinderCreateName = "";
    }

    public FlowbinderCreatePlace(String flowbinderCreateName) {
        this.flowbinderCreateName = flowbinderCreateName;
    }

    public String getFlowbinderCreateName() {
        return flowbinderCreateName;
    }

    @Prefix("CreateFlowbinder")
    public static class Tokenizer implements PlaceTokenizer<FlowbinderCreatePlace> {
        @Override
        public String getToken(FlowbinderCreatePlace place) {
            return place.getFlowbinderCreateName();
        }

        @Override
        public FlowbinderCreatePlace getPlace(String token) {
            return new FlowbinderCreatePlace(token);
        }
    }
    
}
