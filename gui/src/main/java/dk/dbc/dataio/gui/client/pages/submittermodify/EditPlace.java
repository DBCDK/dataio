/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.submittermodify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.commons.types.Submitter;


/**
 * EditPlace
 */
public class EditPlace extends Place {
    private Long submitterId;

    public EditPlace(String url) {
        this.submitterId = Long.valueOf(url);
    }

    public EditPlace(Submitter submitter) {
        this.submitterId = submitter.getId();
    }

    public Long getSubmitterId() {
        return submitterId;
    }

    @Prefix("EditSubmitter")
    public static class Tokenizer implements PlaceTokenizer<EditPlace> {
        @Override
        public String getToken(EditPlace place) {
            return String.valueOf(place.getSubmitterId());
        }
        @Override
        public EditPlace getPlace(String token) {
            return new EditPlace(token);
        }
    }

}
