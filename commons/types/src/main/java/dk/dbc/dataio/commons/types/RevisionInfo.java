package dk.dbc.dataio.commons.types;

import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The RevisionInfo class represents information about a committed revision in the source control management system.
 * <p>
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 * Also due to GWT compatibility no java7 constructs are allowed,
 */
public class RevisionInfo implements Serializable {
    private static final long serialVersionUID = -462330724031235706L;

    private /* final */ long revision;
    private /* final */ String author;
    private /* final */ String message;
    private /* final */ Date date;
    private /* final */ List<ChangedItem> changedItems;

    private RevisionInfo() {
    }

    /**
     * Class constructor
     *
     * @param revision     number of the revision that this object represents
     * @param author       author of the revision that this object represents
     * @param date         datestamp when the revision was committed
     * @param message      log message attached to the revision
     * @param changedItems list of items changed in the revision
     * @throws NullPointerException if given null-valued argument
     */
    public RevisionInfo(long revision, String author, Date date, String message, List<ChangedItem> changedItems)
            throws NullPointerException {
        this.revision = revision;
        this.author = InvariantUtil.checkNotNullOrThrow(author, "author");
        this.message = InvariantUtil.checkNotNullOrThrow(message, "message");
        this.date = new Date(InvariantUtil.checkNotNullOrThrow(date, "date").getTime());
        this.changedItems = new ArrayList<>(InvariantUtil.checkNotNullOrThrow(changedItems, "changedItems"));
    }

    public String getAuthor() {
        return author;
    }

    public List<ChangedItem> getChangedItems() {
        return new ArrayList<>(changedItems);
    }

    public Date getDate() {
        return new Date(date.getTime());
    }

    public String getMessage() {
        return message;
    }

    public long getRevision() {
        return revision;
    }

    /**
     * The ChangedItem class represents a single item changed in a revision
     * <p>
     * In all essence objects of this class are immutable, but due to GWT serialization
     * issues we cannot have final fields and need a default no-arg constructor.
     * Also due to GWT compatibility no java7 constructs are allowed,
     */
    public static class ChangedItem implements Serializable {
        private static final long serialVersionUID = 2946860014066124172L;

        private /* final */ String path;
        private /* final */ String type;

        private ChangedItem() {
        }

        /**
         * Class constructor
         *
         * @param path path of the item represented by this object
         * @param type type of the change applied to the item represented by this object
         * @throws NullPointerException     if given null-valued argument
         * @throws IllegalArgumentException if given empty-valued argument
         */
        public ChangedItem(String path, String type)
                throws NullPointerException, IllegalArgumentException {
            this.path = InvariantUtil.checkNotNullNotEmptyOrThrow(path, "path");
            this.type = InvariantUtil.checkNotNullNotEmptyOrThrow(type, "type");
        }

        public String getPath() {
            return path;
        }

        public String getType() {
            return type;
        }
    }
}
