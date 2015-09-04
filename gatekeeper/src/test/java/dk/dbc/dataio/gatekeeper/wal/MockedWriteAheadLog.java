package dk.dbc.dataio.gatekeeper.wal;

import java.util.LinkedList;
import java.util.List;

public class MockedWriteAheadLog implements WriteAheadLog {
    public LinkedList<Modification> modifications = new LinkedList<>();
    public int modificationsAddedOverTime = 0;

    @Override
    public void add(List<Modification> modifications) {
        modificationsAddedOverTime += modifications.size();
        this.modifications.addAll(modifications);
    }

    @Override
    public Modification next() throws ModificationLockedException {
        if (!modifications.isEmpty()) {
            final Modification modification = modifications.peek();
            modification.lock();
            return modification;
        }
        return null;
    }

    @Override
    public void delete(Modification modification) {
        modifications.remove(modification);
    }

    @Override
    public boolean unlock(Modification modification) {
        if (modification.isLocked()) {
            modification.unlock();
            return true;
        }
        return false;
    }
}
