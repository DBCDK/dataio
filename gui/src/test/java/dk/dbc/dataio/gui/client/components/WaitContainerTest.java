package dk.dbc.dataio.gui.client.components;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * WaitContainer unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class WaitContainerTest {

    private class DummyClass {
        private String s;
        private Integer i;

        public DummyClass(String s, Integer i) {
            this.s = s;
            this.i = i;
        }

        public String getS() {
            return s;
        }

        public Integer getI() {
            return i;
        }
    }


    // Constructor test

    @Test
    public void constructor_nullLambdaNoKeys_noException() {
        // Test Object
        new WaitContainer(null);
    }

    @Test
    public void constructor_nullLambdaNullKey_noException() {
        // Test Object
        new WaitContainer(null, (String) null);
    }

    @Test
    public void constructor_nullLambdaTwoKeys_noException() {
        // Test Object
        new WaitContainer(null, "uniqueKeyA", "uniqueKeyB");
    }

    @Test
    public void constructor_validLambdaNoKeys_noExceptionAndNoCallback() {
        // Test Object
        new WaitContainer(keys -> {
            assert (false);
        });
    }

    @Test
    public void constructor_validLambdaNullKey_noExceptionAndNoCallback() {
        // Test Object
        new WaitContainer(keys -> {
            assert (false);
        }, (String) null);
    }

    @Test
    public void constructor_validLambdaTwoKeys_noExceptionAndNoCallback() {
        // Test Object
        new WaitContainer(keys -> {
            assert (false);
        }, "uniqueKeyA", "uniqueKeyB");
    }


    // put test

    @Test
    public void put_nullCallback_noCallbackNoException() {
        // Test Preparation
        WaitContainer waitContainer = new WaitContainer(null, "uniqueKeyA");

        // Test Object
        waitContainer.put("uniqueKeyA", "sample");
    }

    @Test
    public void put_validCallbackOneElement_callback() {
        // Test Preparation
        WaitContainer waitContainer = new WaitContainer(list -> {
            assertThat(list, is(notNullValue()));
            assertThat(list.size(), is(1));
            Object element = list.get("uniqueKeyA");
            assertThat(element, is("sample"));
        }, "uniqueKeyA");

        // Test Object
        waitContainer.put("uniqueKeyA", "sample");
    }

    @Test
    public void put_validCallbackTwoElementsOnlyOnePut_noCallback() {
        // Test Preparation
        WaitContainer waitContainer = new WaitContainer(list -> {
            assert (false);
        }, "uniqueKeyA", "B");

        // Test Object
        waitContainer.put("uniqueKeyA", "sample");
    }

    @Test
    public void put_validCallbackTwoElementsTwoPuts_callback() {
        // Test Preparation
        WaitContainer waitContainer = new WaitContainer(list -> {
            assertThat(list, is(notNullValue()));
            assertThat(list.size(), is(2));
            Object first = list.get("uniqueKeyA");
            assertThat(first, is("sample"));
            DummyClass second = (DummyClass) list.get("B");
            assertThat(second.getS(), is("Hello"));
            assertThat(second.getI(), is(127));
        }, "uniqueKeyA", "B");

        // Test Object
        waitContainer.put("uniqueKeyA", "sample");
        waitContainer.put("B", new DummyClass("Hello", 127));
    }

    @Test
    public void put_validCallbackTwoElementsOneValidOneUnknownPuts_noCallback() {
        // Test Preparation
        WaitContainer waitContainer = new WaitContainer(list -> {
            assert (false);
        }, "uniqueKeyA", "B");

        // Test Object
        waitContainer.put("uniqueKeyA", "sample");
        waitContainer.put("C", new DummyClass("Hello", 127));  // "C" is not a valid key
    }

}
