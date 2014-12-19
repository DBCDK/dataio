package dk.dbc.dataio.gui.client.pages.job.show;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class ViewHelperTest {

    /*
     * Testing starts here...
     */

    // validateObjects(...)
    @Test
    public void validateObjects_validObjects_returnTrue() {
        assertThat(ViewHelper.validateObjects("Object 1", "Object 2"), is(true));
    }

    @Test
    public void validateObjects_oneNullObject_returnFalse() {
        assertThat(ViewHelper.validateObjects(null, "Object 2"), is(false));
        assertThat(ViewHelper.validateObjects("Object 1", null), is(false));
    }

    @Test
    public void validateObjects_twoNullObjects_returnFalse() {
        assertThat(ViewHelper.validateObjects(null, null), is(false));
    }


    // compareStringsAsLongs(...)
    @Test
    public void compareStringsAsLongs_lessThan_returnNegative() {
        assertThat(ViewHelper.compareStringsAsLongs("11", "22"), lessThan(0));
        assertThat(ViewHelper.compareStringsAsLongs("3", "22"), lessThan(0));
    }

    @Test
    public void compareStringsAsLongs_equalTo_returnZero() {
        assertThat(ViewHelper.compareStringsAsLongs("11", "11"), is(0));
    }

    @Test
    public void compareStringsAsLongs_greaterThan_returnPositive() {
        assertThat(ViewHelper.compareStringsAsLongs("33", "22"), greaterThan(0));
        assertThat(ViewHelper.compareStringsAsLongs("33", "4"), greaterThan(0));
    }


    // compareStrings
    @Test
    public void compareStrings_lessThan_returnNegative() {
        assertThat(ViewHelper.compareStrings("11", "22"), lessThan(0));
        assertThat(ViewHelper.compareStrings("111", "22"), lessThan(0));
    }

    @Test
    public void compareStrings_equalTo_returnZero() {
        assertThat(ViewHelper.compareStrings("11", "11"), is(0));
    }

    @Test
    public void compareStrings_greaterThan_returnPositive() {
        assertThat(ViewHelper.compareStrings("33", "22"), greaterThan(0));
        assertThat(ViewHelper.compareStrings("33", "222"), greaterThan(0));
    }


    // compareLongDates
    @Test
    public void compareLongDates_lessThan_returnNegative() {
        assertThat(ViewHelper.compareStrings("2014-11-18 00:36:37", "2015-11-18 00:36:37"), lessThan(0));
        assertThat(ViewHelper.compareStrings("2014-11-18 00:36:37", "2014-12-18 00:36:37"), lessThan(0));
        assertThat(ViewHelper.compareStrings("2014-11-18 00:36:37", "2014-11-19 00:36:37"), lessThan(0));
        assertThat(ViewHelper.compareStrings("2014-11-18 00:36:37", "2014-11-18 01:36:37"), lessThan(0));
        assertThat(ViewHelper.compareStrings("2014-11-18 00:36:37", "2014-11-18 00:37:37"), lessThan(0));
        assertThat(ViewHelper.compareStrings("2014-11-18 00:36:37", "2014-11-18 00:36:38"), lessThan(0));
    }

    @Test
    public void compareLongDates_equalTo_returnZero() {
        assertThat(ViewHelper.compareStrings("2014-11-18 00:36:37", "2014-11-18 00:36:37"), is(0));
    }

    @Test
    public void compareLongDates_greaterThan_returnPositive() {
        assertThat(ViewHelper.compareStrings("2015-11-18 00:36:37", "2014-11-18 00:36:37"), greaterThan(0));
        assertThat(ViewHelper.compareStrings("2014-12-18 00:36:37", "2014-11-18 00:36:37"), greaterThan(0));
        assertThat(ViewHelper.compareStrings("2014-11-19 00:36:37", "2014-11-18 00:36:37"), greaterThan(0));
        assertThat(ViewHelper.compareStrings("2014-11-18 01:36:37", "2014-11-18 00:36:37"), greaterThan(0));
        assertThat(ViewHelper.compareStrings("2014-11-18 00:37:37", "2014-11-18 00:36:37"), greaterThan(0));
        assertThat(ViewHelper.compareStrings("2014-11-18 00:36:38", "2014-11-18 00:36:37"), greaterThan(0));
    }



}