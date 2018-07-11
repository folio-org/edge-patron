package org.folio.edge.patron.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.folio.edge.core.utils.Mappers;

import javax.annotation.Generated;
import java.io.IOException;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "error")
@JsonDeserialize(builder = ErrorMessage.Builder.class)
@JsonPropertyOrder({
        "code",
        "message"
})
public class ErrorMessage {

    @JsonProperty("code")
    public final Integer httpStatusCode;

    @JsonProperty("message")
    public final String errorMessage;

    public ErrorMessage(int statusCode, String errorMsg){
        this.httpStatusCode = statusCode;
        this.errorMessage = errorMsg;
    }

    @Override
    @Generated("Eclipse")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ErrorMessage)) {
            return false;
        }
        ErrorMessage other = (ErrorMessage) obj;

        if (this.httpStatusCode == null) {
            if (other.httpStatusCode != null) {
                return false;
            }
        } else if (!httpStatusCode.equals(other.httpStatusCode)) {
            return false;
        }

        if (this.errorMessage == null) {
            if (other.errorMessage != null) {
                return false;
            }
        } else if (!errorMessage.equals(other.errorMessage)) {
            return false;
        }

        return true;
    }

    public String toXml() throws JsonProcessingException {
        return Mappers.XML_PROLOG + Mappers.xmlMapper.writeValueAsString(this);
    }

    public String toJson() throws JsonProcessingException {
        return Mappers.jsonMapper.writeValueAsString(this);
    }

    public static ErrorMessage fromJson(String json) throws IOException {
        return Mappers.jsonMapper.readValue(json, ErrorMessage.class);
    }

    public static ErrorMessage fromXml(String xml) throws IOException{
        return Mappers.xmlMapper.readValue(xml, ErrorMessage.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer httpStatusCode;
        private String errorMessage;

        @JsonProperty("code")
        public Builder item(Integer httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
            return this;
        }

        @JsonProperty("message")
        public Builder chargeAmount(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ErrorMessage build() {
            return new ErrorMessage(httpStatusCode, errorMessage);
        }
    }
}
