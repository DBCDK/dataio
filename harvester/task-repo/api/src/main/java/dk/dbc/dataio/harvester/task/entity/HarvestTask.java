package dk.dbc.dataio.harvester.task.entity;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvestTaskSelector;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "task")
@NamedNativeQueries({
        @NamedNativeQuery(name = HarvestTask.QUERY_FIND_NEXT,
                query = "SELECT * from task WHERE configId = ?configId ORDER BY id ASC FOR UPDATE SKIP LOCKED",
                resultClass = HarvestTask.class),
})
public class HarvestTask {
    public static final String QUERY_FIND_NEXT = "HarvestTask.next";

    @Id
    @SequenceGenerator(
            name = "task_id_seq",
            sequenceName = "task_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "task_id_seq")
    @Column(updatable = false)
    private int id;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfCreation;

    private Long configId;
    private Integer basedOnJob;
    private Integer numberOfRecords;

    @Column(columnDefinition = "json")
    @Convert(converter = AddiMetaDataListConverter.class)
    private List<AddiMetaData> records;

    @Convert(converter = HarvestTaskSelectorConverter.class)
    private HarvestTaskSelector selector;

    public HarvestTask() {
    }

    public int getId() {
        return id;
    }

    public Timestamp getTimeOfCreation() {
        return timeOfCreation;
    }

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public Integer getBasedOnJob() {
        return basedOnJob;
    }

    public void setBasedOnJob(Integer basedOnJob) {
        this.basedOnJob = basedOnJob;
    }

    public Integer getNumberOfRecords() {
        return numberOfRecords;
    }

    public void setNumberOfRecords(Integer numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
    }

    public List<AddiMetaData> getRecords() {
        return records;
    }

    public void setRecords(List<AddiMetaData> records) {
        this.records = records;
    }

    public HarvestTaskSelector getSelector() {
        return selector;
    }

    public void setSelector(HarvestTaskSelector selector) {
        this.selector = selector;
    }
}
