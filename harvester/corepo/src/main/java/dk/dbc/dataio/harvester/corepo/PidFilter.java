package dk.dbc.dataio.harvester.corepo;

import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.invariant.InvariantUtil;

import java.util.Set;
import java.util.function.Predicate;

public class PidFilter implements Predicate<Pid> {

    private final Set<Integer> agencyIds;

    public PidFilter(Set<Integer> agencyIds) throws NullPointerException {
        this.agencyIds = InvariantUtil.checkNotNullOrThrow(agencyIds, "agencyIds");
    }

    @Override
    public boolean test(Pid pid) {
        return pid.getType() == Pid.Type.BIBLIOGRAPHIC_OBJECT && agencyIds.contains(pid.getAgencyId());
    }
}
