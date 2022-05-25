package dk.dbc.dataio.gatekeeper.wal;

public class ModificationLockedException extends Exception {
    private static final long serialVersionUID = 1141197919982221347L;

    public ModificationLockedException(String message) {
        super(message);
    }
}
