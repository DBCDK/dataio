package dk.dbc.dataio.gui.client.pages.submittercreate;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface SubmitterCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_SubmitterNumber();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_SubmitterName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Description();


    // Buttons
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Save();


    // Error messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_InputFieldValidationError();
    
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_NumberInputFieldValidationError();
    
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_ProxyKeyViolationError();
    
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_ProxyDataValidationError();
    
    
    // Status messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_SubmitterSuccessfullySaved();
    
}
