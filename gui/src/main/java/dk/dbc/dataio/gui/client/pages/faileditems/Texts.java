package dk.dbc.dataio.gui.client.pages.faileditems;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {

    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_JobId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_ChunkId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_ItemId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Chunkify();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Processing();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Delivery();


    // Status
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_success();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_failure();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_ignored();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_incomplete();


    // Error messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFetchJobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFetchJobCompletionStatusFor();

}
