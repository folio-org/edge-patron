package org.folio.edge.patron.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.folio.edge.core.utils.Mappers;

import java.io.IOException;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "holdCancellation")
@JsonDeserialize(builder = HoldCancellation.Builder.class)
@JsonPropertyOrder({
    "holdId",
    "cancelationReasonId",
    "canceledByUserId",
    "cancelationAdditionalInformation",
    "cancelationDate"
})
public final class HoldCancellation {
  public final String holdId;
  public final String cancellationReasonId;
  public final String canceledByUserId;
  public final String cancellationAdditionalInformation;
  public final Date canceledDate;

  private HoldCancellation(Builder builder) {
    this.holdId = builder.holdId;
    this.canceledDate = builder.canceledDate;
    this.canceledByUserId = builder.canceledByUserId;
    this.cancellationReasonId = builder.cancellationReasonId;
    this.cancellationAdditionalInformation = builder.cancellationAdditionalInformation;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    @JsonProperty("holdId")
    private String holdId;

    @JsonProperty("cancellationReasonId")
    private String cancellationReasonId;

    @JsonProperty("canceledByUserId")
    private String canceledByUserId;

    @JsonProperty("cancellationAdditionalInformation")
    private String cancellationAdditionalInformation;

    @JsonProperty("canceledDate")
    private Date canceledDate;

    public Builder holdId(String holdId) {
      this.holdId = holdId;
      return this;
    }

    public Builder canceledDate(Date canceledDate) {
      this.canceledDate = canceledDate;
      return this;
    }

    public Builder cancellationReasonId(String cancellationReasonId) {
      this.cancellationReasonId = cancellationReasonId;
      return this;
    }

    public Builder cancellationAdditionalInformation(String cancellationAdditionalInformation) {
      this.cancellationAdditionalInformation = cancellationAdditionalInformation;
      return this;
    }

    public Builder canceledByUserId(String canceledByUserId) {
      this.canceledByUserId = canceledByUserId;
      return this;
    }

    public HoldCancellation build() {
      return new HoldCancellation(this);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((holdId == null) ? 0 : holdId.hashCode());
    result = prime * result + ((cancellationReasonId == null) ? 0 : cancellationReasonId.hashCode());
    result = prime * result + ((canceledByUserId == null) ? 0 : canceledByUserId.hashCode());
    result = prime * result + ((cancellationAdditionalInformation == null) ? 0 : cancellationAdditionalInformation.hashCode());
    result = prime * result + ((canceledDate == null) ? 0 : canceledDate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof HoldCancellation)) {
      return false;
    }
    HoldCancellation other = (HoldCancellation) obj;

    if (holdId == null) {
      if (other.holdId != null) {
        return false;
      }
    } else if (!holdId.equals(other.holdId)) {
      return false;
    }
    if (cancellationReasonId == null) {
      if (other.cancellationReasonId != null) {
        return false;
      }
    } else if (!cancellationReasonId.equals(other.cancellationReasonId)) {
      return false;
    }
    if (canceledByUserId == null) {
      if (other.canceledByUserId != null) {
        return false;
      }
    } else if (!canceledByUserId.equals(other.canceledByUserId)) {
      return false;
    }
    if (cancellationAdditionalInformation == null) {
      if (other.cancellationAdditionalInformation != null) {
        return false;
      }
    } else if (!cancellationAdditionalInformation.equals(other.cancellationAdditionalInformation)) {
      return false;
    }
    if (canceledDate == null) {
      if (other.canceledDate != null) {
        return false;
      }
    } else if (!canceledDate.equals(other.canceledDate)) {
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

  public static HoldCancellation fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, HoldCancellation.class);
  }

  public static HoldCancellation fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, HoldCancellation.class);
  }
}
