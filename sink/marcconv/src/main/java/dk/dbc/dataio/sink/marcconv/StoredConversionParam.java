package dk.dbc.dataio.sink.marcconv;

import dk.dbc.dataio.commons.conversion.ConversionParam;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "param")
@NamedQueries({
        @NamedQuery(
                name = StoredConversionParam.DELETE_CONVERSION_PARAM_QUERY_NAME,
                query = StoredConversionParam.DELETE_CONVERSION_PARAM_QUERY)
})
public class StoredConversionParam {
    public static final String DELETE_CONVERSION_PARAM_QUERY =
            "DELETE FROM StoredConversionParam param" +
                    " WHERE param.jobId = :jobId";
    public static final String DELETE_CONVERSION_PARAM_QUERY_NAME =
            "StoredConversionParam.delete";

    @Id
    private Integer jobId;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = ConversionParamConverter.class)
    private ConversionParam param;

    public StoredConversionParam() {
    }

    StoredConversionParam(Integer jobId) {
        this.jobId = jobId;
    }

    public Integer getJobId() {
        return jobId;
    }

    public ConversionParam getParam() {
        return param;
    }

    public void setParam(ConversionParam param) {
        this.param = param;
    }

    @Override
    public String toString() {
        return "StoredConversionParam{" +
                "jobId=" + jobId +
                ", param=" + param +
                '}';
    }
}
