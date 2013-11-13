package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.sink.InvalidMessageSinkException;
import org.junit.Test;

/**
 * EsMessageProcessorBean unit tests.
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class EsMessageProcessorBeanTest {
    @Test
    public void onMessage_messageArgIsNull_messageDrivenContextIsNotAccessed() {
        // We utilize the fact that since no MessageDrivenContext has been injected
        // any access would throw java.lang.NullPointerException
        getInitializedBean().onMessage(null);
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgIsNull_throws() throws InvalidMessageSinkException {
        getInitializedBean().validateMessage(null);
    }

    private static EsMessageProcessorBean getInitializedBean() {
        return new EsMessageProcessorBean();
    }
}
