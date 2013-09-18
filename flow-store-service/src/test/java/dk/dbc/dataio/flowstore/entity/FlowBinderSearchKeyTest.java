package dk.dbc.dataio.flowstore.entity;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

/**
 * FlowBinderSearchKey unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowBinderSearchKeyTest {
    private final String packaging = "packaging";
    private final String format = "format";
    private final String charset = "charset";
    private final String destination = "destination";
    private final Long submitter = 42L;

    @Test(expected = NullPointerException.class)
    public void constructor_packagingArgIsNull_throws() {
        new FlowBinderSearchKey(null, format, charset, destination, submitter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_packagingArgIsEmpty_throws() {
        new FlowBinderSearchKey("", format, charset, destination, submitter);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_formatArgIsNull_throws() {
        new FlowBinderSearchKey(packaging, null, charset, destination, submitter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_formatArgIsEmpty_throws() {
        new FlowBinderSearchKey(packaging, "", charset, destination, submitter);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_charsetArgIsNull_throws() {
        new FlowBinderSearchKey(packaging, format, null, destination, submitter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_charsetArgIsEmpty_throws() {
        new FlowBinderSearchKey(packaging, format, "", destination, submitter);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_destinationArgIsNull_throws() {
        new FlowBinderSearchKey(packaging, format, charset, null, submitter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_destinationArgIsEmpty_throws() {
        new FlowBinderSearchKey(packaging, format, charset, "", submitter);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_submitterArgIsNull_throws() {
        new FlowBinderSearchKey(packaging, format, charset, destination, null);
    }

    @Test
    public void class_upholdsEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(FlowBinderSearchKey.class)
                .suppress(Warning.NULL_FIELDS)
                .verify();
    }
}
