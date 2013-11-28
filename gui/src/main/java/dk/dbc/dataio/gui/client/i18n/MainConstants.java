package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;

public interface MainConstants extends Constants {
    public static final String TRANSLATED_TEXT_IS_MISSING = "<translated text is missing>";
    
    // Headers
    @DefaultStringValue(TRANSLATED_TEXT_IS_MISSING)
    String header_DataIO();
    
    @DefaultStringValue(TRANSLATED_TEXT_IS_MISSING)
    String header_FlowCreation();
    
    @DefaultStringValue(TRANSLATED_TEXT_IS_MISSING)
    String header_FlowComponentCreation();
    
    @DefaultStringValue(TRANSLATED_TEXT_IS_MISSING)
    String header_SubmitterCreation();
    
    @DefaultStringValue(TRANSLATED_TEXT_IS_MISSING)
    String header_FlowbinderCreation();
    
    @DefaultStringValue(TRANSLATED_TEXT_IS_MISSING)
    String header_SinkCreation();
    
    @DefaultStringValue(TRANSLATED_TEXT_IS_MISSING)
    String header_FlowComponentsShow();
    
}
