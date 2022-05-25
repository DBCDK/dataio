package dk.dbc.dataio.gui.client.components.multilist;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;

/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class MultiListTest {

    @Test
    public void clear_callClear_clearList() {
        MultiList multiList = new MultiList();

        multiList.clear();

        verify(multiList.list).clear();
    }

    @Test
    public void addValue_callAddValue_multiListAddCalled() {
        final String TEXT = "-text-";
        final String KEY = "-key-";

        MultiList multiList = new MultiList();

        multiList.addValue(TEXT, KEY);

        verify(multiList.list).add(TEXT, KEY);
    }

    @Test
    public void enable_callsetEnabled_multiListSetEnabledCalled() {
        MultiList multiList = new MultiList();

        multiList.setEnabled(true);
        verify(multiList.list).setEnabled(true);

        multiList.setEnabled(false);
        verify(multiList.list).setEnabled(false);
    }

    @Test
    public void isAddEvent_callIsAddEvent_check() {
        final MultiList multiList = new MultiList();

        final ClickEvent addEvent = new ClickEvent() {
            {
                super.setSource(multiList.addButton);
            }
        };
        final ClickEvent nonAddEvent = new ClickEvent() {
            {
                super.setSource(multiList.removeButton);
            }
        };
        assertThat(multiList.isAddEvent(addEvent), is(true));
        assertThat(multiList.isAddEvent(nonAddEvent), is(false));
    }

    @Test
    public void isRemoveEvent_callIsRemoveEvent_check() {
        final MultiList multiList = new MultiList();

        final ClickEvent addEvent = new ClickEvent() {
            {
                super.setSource(multiList.addButton);
            }
        };
        final ClickEvent nonAddEvent = new ClickEvent() {
            {
                super.setSource(multiList.removeButton);
            }
        };
        assertThat(multiList.isRemoveEvent(addEvent), is(false));
        assertThat(multiList.isRemoveEvent(nonAddEvent), is(true));
    }

}
