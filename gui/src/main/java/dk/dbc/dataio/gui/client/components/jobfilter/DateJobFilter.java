package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.gui.client.components.prompted.PromptedDateTimeBox;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.Date;

/**
 * This is the Date Job Filter
 */
public class DateJobFilter extends BaseJobFilter {
    private final static Integer ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;
    private final static String DEFAULT_TO_DATE = "";
    private final static String DEFAULT_EMPTY_TIME = "00:00:00";
    private static final String INTEGER_MATCHER = "^\\d+$";

    interface DateJobFilterUiBinder extends UiBinder<HTMLPanel, DateJobFilter> {
    }

    private static DateJobFilterUiBinder ourUiBinder = GWT.create(DateJobFilterUiBinder.class);

    ChangeHandler callbackChangeHandler = null;


    @UiField
    PromptedDateTimeBox fromDate;
    @UiField
    PromptedDateTimeBox toDate;


    @SuppressWarnings("unused")
    @UiConstructor
    public DateJobFilter() {
        this("", false);
    }

    DateJobFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    DateJobFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        fromDate.setValue(daysFromNow(2));
        toDate.setValue(DEFAULT_TO_DATE);
        setParameter(parameter);
    }

    /**
     * Event handler for handling changes in the selection of from and to dates
     *
     * @param event The ValueChangeEvent
     */
    @SuppressWarnings("unused")
    @UiHandler(value = {"fromDate", "toDate"})
    void dateChanged(ValueChangeEvent<String> event) {
        filterChanged();
        if (callbackChangeHandler != null) {
            callbackChangeHandler.onChange(null);
        }
    }


    /*
     * Abstract methods from BaseJobFilter
     */

    /**
     * Fetches the name of this filter
     *
     * @return The name of the filter
     */
    @Override
    public String getName() {
        return texts.jobDateFilter_name();
    }

    /**
     * Gets the value of the job filter, which is the constructed JobListCriteria to be used in the filter search
     *
     * @return The constructed JobListCriteria filter
     */
    @Override
    public JobListCriteria getValue() {
        CriteriaClass criteriaClass = new CriteriaClass();
        criteriaClass.add(ListFilter.Op.GREATER_THAN, fromDate.getValue());
        criteriaClass.add(ListFilter.Op.LESS_THAN, toDate.getValue());
        return criteriaClass.getCriteria();
    }

    /**
     * Sets the selection according to the value, setup in the parameter attribute<br>
     * The value is one or two date parameters, separated by commas<br>
     * Each date parameter can be entered in two different formats:<br>
     * <ul>
     *      <li>An integer: Giving a number of days to subtract from the current date</li>
     *      <li>A date format in the format: 'yyyy-MM-dd HH:mm:ss'</li>
     * </ul>
     *
     * @param filterParameter The filter parameters to be used by this job filter
     */
    @Override
    public void localSetParameter(String filterParameter) {
        if (!filterParameter.isEmpty()) {
            String[] data = filterParameter.split(",", 2);
            fromDate.setValue(evaluateDate(data[0]), true);
            if (data.length == 2) {
                toDate.setValue(evaluateDate(data[1]), true);
            }
        }
    }

    /**
     * Gets the parameter value for the filter
     *
     * @return The stored filter parameter for the specific job filter
     */
    @Override
    public String getParameter() {
        return fromDate.getValue() + (toDate.getValue().isEmpty() ? "" : "," + toDate.getValue());
    }


    /*
     * Override HasChangeHandlers Interface Methods
     */

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        callbackChangeHandler = changeHandler;
        callbackChangeHandler.onChange(null);
        return () -> callbackChangeHandler = null;
    }


    /*
     * Private
     */

    /**
     * Calculates the default "from" date to be used. Should be set to the current day minus two days, at 00:00:00
     *
     * @param days Gives the number of days to subtract from the current date
     * @return The default From date
     */
    private String daysFromNow(int days) {
        String date = Format.formatLongDate(new Date(System.currentTimeMillis() - days * ONE_DAY_IN_MILLISECONDS));
        return date.substring(0, date.length() - DEFAULT_EMPTY_TIME.length()) + DEFAULT_EMPTY_TIME;
    }

    /**
     * Evaluates a string, and returns a proper date value in the form 'yyyy-MM-dd HH:mm:ss'
     *
     * @param date The input string to evaluate
     * @return The resulting date
     */
    private String evaluateDate(String date) {
        if (date == null || date.isEmpty()) {
            return date;
        }
        if (date.matches(INTEGER_MATCHER)) {  // Then this is an integer, giving the number of days to subtract from the current date
            return daysFromNow(Integer.valueOf(date));
        }
        return date;  // The default case is the format 'yyyy-MM-dd HH:mm:ss'
    }


    /*
     * Private classes
     */

    /**
     * This class is used to construct a composite JobListCriteria
     */
    private class CriteriaClass {
        boolean isFirstCriteria = true;
        private JobListCriteria criteria = new JobListCriteria();

        public void add(ListFilter.Op operator, String date) {
            if (date != null && !date.isEmpty()) {
                if (isFirstCriteria) {
                    isFirstCriteria = false;
                    criteria.where(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, operator, Format.parseLongDateAsDate(date)));
                } else {
                    criteria.and(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, operator, Format.parseLongDateAsDate(date)));
                }
            }
        }

        public JobListCriteria getCriteria() {
            return criteria;
        }
    }

}
