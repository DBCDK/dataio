package dk.dbc.dataio.gui.client.components.MultiList;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

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

}
