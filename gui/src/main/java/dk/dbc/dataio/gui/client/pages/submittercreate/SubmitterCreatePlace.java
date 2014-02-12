/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.submittercreate;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 *
 * @author slf
 */
public class SubmitterCreatePlace extends Place {
    private String submitterCreateName;

    public SubmitterCreatePlace() {
        this.submitterCreateName = "";
    }

    public SubmitterCreatePlace(String submitterCreateName) {
        this.submitterCreateName = submitterCreateName;
    }

    public String getSubmitterCreateName() {
        return submitterCreateName;
    }

    @Prefix("CreateSubmitter")
    public static class Tokenizer implements PlaceTokenizer<SubmitterCreatePlace> {
        @Override
        public String getToken(SubmitterCreatePlace place) {
            return place.getSubmitterCreateName();
        }

        @Override
        public SubmitterCreatePlace getPlace(String token) {
            return new SubmitterCreatePlace(token);
        }
    }
    
}
