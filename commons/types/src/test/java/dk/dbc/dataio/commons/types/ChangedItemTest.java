package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * ChangedItem unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */

public class ChangedItemTest {
    private final String path = "path";
    private final String type = "type";

    @Test(expected = NullPointerException.class)
    public void constructor_pathArgIsNull_throws() {
        new RevisionInfo.ChangedItem(null, type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_pathArgIsEmpty_throws() {
        new RevisionInfo.ChangedItem("", type);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_typeArgIsNull_throws() {
        new RevisionInfo.ChangedItem(path, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_typeArgIsEmpty_throws() {
        new RevisionInfo.ChangedItem(path, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final RevisionInfo.ChangedItem instance = new RevisionInfo.ChangedItem(path, type);
        assertThat(instance, is(notNullValue()));
    }
}
