package dk.dbc.dataio.commons.types;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * RevisionInfo unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class RevisionInfoTest {
    private final long revision = 42L;
    private final String author = "author";
    private final String message = "message";
    private final Date date = new Date();
    private final List<RevisionInfo.ChangedItem> changedItems = new ArrayList<>(0);

    @Test(expected = NullPointerException.class)
    public void constructor_authorArgIsNull_throws() {
        new RevisionInfo(revision, null, date, message, changedItems);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_dateArgIsNull_throws() {
        new RevisionInfo(revision, author, null, message, changedItems);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_messageArgIsNull_throws() {
        new RevisionInfo(revision, author, date, null, changedItems);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_changedItemsArgIsNull_throws() {
        new RevisionInfo(revision, author, date, message, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final RevisionInfo instance = new RevisionInfo(revision, author, date, message, changedItems);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void verify_defensiveCopyingOfChangedItemList() {
        final List<RevisionInfo.ChangedItem> changedItems = new ArrayList<>();
        changedItems.add(null);
        final RevisionInfo instance = new RevisionInfo(revision, author, date, message, changedItems);
        assertThat(instance.getChangedItems().size(), is(1));
        changedItems.add(null);
        final List<RevisionInfo.ChangedItem> returnedItems = instance.getChangedItems();
        assertThat(returnedItems.size(), is(1));
        returnedItems.add(null);
        assertThat(instance.getChangedItems().size(), is(1));
    }

    @Test
    public void verify_defensiveCopyingOfDate() {
        final Date datestamp = new Date();
        final long expectedTime = datestamp.getTime();
        final RevisionInfo instance = new RevisionInfo(revision, author, datestamp, message, changedItems);
        datestamp.setTime(42);
        final Date returnedDate = instance.getDate();
        assertThat(returnedDate.getTime(), is(expectedTime));
        returnedDate.setTime(42);
        assertThat(instance.getDate().getTime(), is(expectedTime));
    }
}
