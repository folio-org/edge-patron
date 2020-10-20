package org.folio.edge.patron.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.folio.edge.core.utils.Mappers;

import java.io.IOException;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "error")
@JsonDeserialize(builder = ErrorMessage.Builder.class)
@JsonPropertyOrder({
        "code",
        "errorMessage"
})
public class ErrorMessage {

    @JsonProperty("code")
    public final int httpStatusCode;

    @JsonProperty("errorMessage")
    public final String message;

    public ErrorMessage(int statusCode, String errorMsg){
        this.httpStatusCode = statusCode;
        this.message = errorMsg;
    }

    @Override
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

        if (httpStatusCode != other.httpStatusCode)
            return false;

        if (this.message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!message.equals(other.message)) {
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

        private int httpStatusCode;
        private String message;

        @JsonProperty("code")
        public Builder item(int httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
            return this;
        }

        @JsonProperty("errorMessage")
        public Builder chargeAmount(String errorMessage) {
            this.message = errorMessage;
            return this;
        }

        public ErrorMessage build() {
            return new ErrorMessage(httpStatusCode, message);
        }
    }
}
