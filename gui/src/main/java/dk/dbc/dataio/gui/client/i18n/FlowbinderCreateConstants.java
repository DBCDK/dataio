package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;

public interface FlowbinderCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue("Flowbinder navn")
    String label_FlowBinderName();

    @DefaultStringValue("Beskrivelse")
    String label_FlowBinderDescription();
    
    @DefaultStringValue("Rammeformat")
    String label_FrameFormat();
    
    @DefaultStringValue("Indholdsformat")
    String label_ContentFormat();
    
    @DefaultStringValue("Tegnsæt")
    String label_CharacterSet();
    
    @DefaultStringValue("Destination")
    String label_Sink();
    
    @DefaultStringValue("Recordsplitter")
    String label_RecordSplitter();
    
    @DefaultStringValue("Submittere")
    String label_Submitters();
    
    @DefaultStringValue("Flow")
    String label_Flow();
    
    @DefaultStringValue("Default Record Splitter")
    String label_DefaultRecordSplitter();
    

    // Tooltips
    @DefaultStringValue("Rammeformat: Teknisk formatprotokol til brug for udveksling af data. Eksempelvis dm2iso, dm2lin, xml, csv, m.v.")
    String tooltip_FrameFormat();

    @DefaultStringValue("Indholdsformat: Bibliografisk format, f.eks. dbc, dfi, dkbilled, dsd, ebogsbib, ebrary, mv.")
    String tooltip_ContentFormat();

    @DefaultStringValue("Tegnsæt: F.eks. utf8, latin-1, samkat, m.v.")
    String tooltip_CharacterSet();

    
    // Error messages
    @DefaultStringValue("Alle felter skal udfyldes.")
    String error_InputFieldValidationError();
    
    @DefaultStringValue("Du forsøger at oprette en Flowbinder, der allerede eksisterer")
    String error_FlowbinderAlreadyExistsError();

    
    // Status messages
    @DefaultStringValue("Flowbinderen blev gemt")
    String status_SaveSuccess();
    
}
