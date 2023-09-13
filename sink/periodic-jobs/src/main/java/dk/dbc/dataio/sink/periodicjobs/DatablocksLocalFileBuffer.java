package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.common.utils.io.UncheckedFileOutputStream;
import dk.dbc.dataio.commons.macroexpansion.MacroSubstitutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.io.File;
import java.io.IOException;

public class DatablocksLocalFileBuffer {
    private EntityManager entityManager;
    private PeriodicJobsDelivery delivery;
    private File tmpFile;
    private MacroSubstitutor macroSubstitutor;

    public DatablocksLocalFileBuffer withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    public DatablocksLocalFileBuffer withDelivery(PeriodicJobsDelivery delivery) {
        this.delivery = delivery;
        return this;
    }

    public DatablocksLocalFileBuffer withTmpFile(File tmpFile) {
        this.tmpFile = tmpFile;
        return this;
    }

    public DatablocksLocalFileBuffer withMacroSubstitutor(MacroSubstitutor macroSubstitutor) {
        this.macroSubstitutor = macroSubstitutor;
        return this;
    }

    public void createLocalFile() {
        String contentHeader = delivery.getConfig().getContent().getPickup().getContentHeader();
        String contentFooter = delivery.getConfig().getContent().getPickup().getContentFooter();

        if (contentHeader != null) {
            contentHeader = macroSubstitutor.replace(contentHeader);
        } else {
            contentHeader = "";
        }

        if (contentFooter != null) {
            contentFooter = macroSubstitutor.replace(contentFooter);
        } else {
            contentFooter = "";
        }

        GroupHeaderIncludePredicate groupHeaderIncludePredicate = new GroupHeaderIncludePredicate();
        Query getDataBlocksQuery = entityManager
                .createNamedQuery(PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME)
                .setParameter(1, delivery.getJobId());
        try (UncheckedFileOutputStream datablocksOutputStream = new UncheckedFileOutputStream(tmpFile);
             ResultSet<PeriodicJobsDataBlock> datablocks = new ResultSet<>(entityManager, getDataBlocksQuery,
                     new PeriodicJobsDataBlockResultSetMapping())) {
            datablocksOutputStream.write(contentHeader.getBytes());
            for (PeriodicJobsDataBlock datablock : datablocks) {
                if (groupHeaderIncludePredicate.test(datablock)) {
                    datablocksOutputStream.write(datablock.getGroupHeader());
                }
                datablocksOutputStream.write(datablock.getBytes());
            }
            datablocksOutputStream.write(contentFooter.getBytes());
            datablocksOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
