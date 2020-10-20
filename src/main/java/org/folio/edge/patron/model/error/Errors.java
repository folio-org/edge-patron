package org.folio.edge.patron.model.error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.folio.edge.core.utils.Mappers;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "errors", "total_records" })
public class Errors {  //NOSONAR

    @JsonProperty("errors")
    private List<Error> errors = new ArrayList<Error>();

    @JsonProperty("total_records")
    private Integer totalRecords;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     *     The errors
     */
    @JsonProperty("errors")
    public List<Error> getErrors() {
        return errors;
    }

    /**
     *
     * @param errors
     *     The errors
     */
    @JsonProperty("errors")
    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public Errors withErrors(List<Error> errors) {
        this.errors = errors;
        return this;
    }

    /**
     *
     * @return
     *     The totalRecords
     */
    @JsonProperty("total_records")
    public Integer getTotalRecords() {
        return totalRecords;
    }

    /**
     *
     * @param totalRecords
     *     The total_records
     */
    @JsonProperty("total_records")
    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Errors withTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Errors withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    public String toJson() throws JsonProcessingException {
      return Mappers.jsonMapper.writeValueAsString(this);
    }
}
