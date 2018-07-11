package org.folio.edge.patron.model.error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.*;
import jdk.nashorn.internal.ir.annotations.Ignore;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "message", "type", "code", "parameters" })
public class Error {  //NOSONAR

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("message")
    private String message;  //NOSONAR

    @JsonProperty("type")
    private String type;

    @JsonProperty("code")
    private String code;

    @JsonProperty("parameters")
    private List<Parameter> parameters = new ArrayList<Parameter>();

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * (Required)
     *
     * @return
     *     The message
     */
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    /**
     *
     * (Required)
     *
     * @param message
     *     The message
     */
    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    public Error withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     *
     * @return
     *     The type
     */
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    /**
     *
     * @param type
     *     The type
     */
    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    public Error withType(String type) {
        this.type = type;
        return this;
    }

    /**
     *
     * @return
     *     The code
     */
    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    /**
     *
     * @param code
     *     The code
     */
    @JsonProperty("code")
    public void setCode(String code) {
        this.code = code;
    }

    public Error withCode(String code) {
        this.code = code;
        return this;
    }

    /**
     *
     * @return
     *     The parameters
     */
    @JsonProperty("parameters")
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     *
     * @param parameters
     *     The parameters
     */
    @JsonProperty("parameters")
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Error withParameters(List<Parameter> parameters) {
        this.parameters = parameters;
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

    public Error withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }
}
