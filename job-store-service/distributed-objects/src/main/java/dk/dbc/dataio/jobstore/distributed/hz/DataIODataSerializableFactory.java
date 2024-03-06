package dk.dbc.dataio.jobstore.distributed.hz;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.hz.processor.RemoveWaitingOnProcessor;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdateStatusProcessor;

import java.util.function.Supplier;

public class DataIODataSerializableFactory implements DataSerializableFactory {
    public static final int FACTORY_ID = 1;

    @Override
    public IdentifiedDataSerializable create(int i) {
        return Objects.values()[i].supplier.get();
    }

    public enum Objects {
        UPDATE_STATUS_PROCESSOR(UpdateStatusProcessor::new),
        STATUS_CHANGE_EVENT(StatusChangeEvent::new),
        TRACKING_KEY(TrackingKey::new),
        REMOVE_WAITING_ON_PROCESSOR(RemoveWaitingOnProcessor::new);

        private final Supplier<IdentifiedDataSerializable> supplier;

        Objects(Supplier<IdentifiedDataSerializable> supplier) {
            this.supplier = supplier;
        }
    }
}
