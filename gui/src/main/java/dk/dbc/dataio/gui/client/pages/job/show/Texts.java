package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {

    // Menu text
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_Jobs();


    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Jobs();


    // Column Headers
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_JobId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_FileName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_SubmitterNumber();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_JobCreationTime();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_JobStatus();


    // Buttons
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_ShowMore();


    // Links
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String link_MoreInfo();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String link_FailedItems();


    // Texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_chunkifying();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_processing();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_delivering();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_done();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_record();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_records();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_failed();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_pending();

}
