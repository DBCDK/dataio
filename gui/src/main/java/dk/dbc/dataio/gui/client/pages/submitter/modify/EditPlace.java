/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;


/**
 * EditPlace
 */
public class EditPlace extends AbstractBasePlace {
    private Long submitterId;

    public EditPlace(String url) {
        this.submitterId = Long.valueOf(url);
    }

    public EditPlace(SubmitterModel model) {
        this.submitterId = model.getId();
    }

    public Long getSubmitterId() {
        return submitterId;
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterEditImpl(this, commonInjector.getMenuTexts().menu_SubmitterEdit());
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
