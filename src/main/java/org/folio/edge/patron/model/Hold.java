package org.folio.edge.patron.model;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;

import org.folio.edge.core.utils.Mappers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "hold")
@JsonDeserialize(builder = Hold.Builder.class)
@JsonPropertyOrder({
    "requestId",
    "item",
    "requestDate",
    "expirationDate",
    "pickupLocationId",
    "status",
    "queuePosition"
})
public final class Hold {
  public final Item item;
  public final String requestId;
  public final Date requestDate;
  public final Date expirationDate;
  public final String pickupLocationId;
  public final Status status;
  public final int queuePosition;

  private Hold(Builder builder) {
    this.item = builder.item;
    this.requestId = builder.requestId;
    this.requestDate = builder.requestDate;
    this.expirationDate = builder.expirationDate;
    this.pickupLocationId = builder.pickupLocationId;
    this.status = builder.status;
    this.queuePosition = builder.queuePosition;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    @JsonProperty("item")
    private Item item;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("requestDate")
    private Date requestDate;

    @JsonProperty("expirationDate")
    private Date expirationDate;

    @JsonProperty("pickupLocationId")
    private String pickupLocationId;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("queuePosition")
    private int queuePosition;

    public Builder item(Item item) {
      this.item = item;
      return this;
    }

    public Builder requestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public Builder requestDate(Date requestDate) {
      this.requestDate = requestDate;
      return this;
    }

    public Builder expirationDate(Date expirationDate) {
      this.expirationDate = expirationDate;
      return this;
    }

    public Builder pickupLocationId(String pickupLocationId) {
      this.pickupLocationId = pickupLocationId;
      return this;
    }

    public Builder status(Status status) {
      this.status = status;
      return this;
    }

    public Builder queuePosition(int queuePosition) {
      this.queuePosition = queuePosition;
      return this;
    }

    public Hold build() {
      return new Hold(this);
    }
  }

  public enum Status {

    OPEN_NOT_YET_FILLED("Open - Not yet filled"), OPEN_AWAITING_PICKUP("Open - Awaiting pickup"), CLOSED_FILLED(
        "Closed - Filled");

    private final String value;
    private static final Map<String, Status> CONSTANTS = new HashMap<>();

    static {
      for (Status c : values()) {
        CONSTANTS.put(c.value, c);
      }
    }

    private Status(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }

    @JsonValue
    public String value() {
      return this.value;
    }

    @JsonCreator
    public static Status fromValue(String value) {
      Status constant = CONSTANTS.get(value);
      if (constant == null) {
        throw new IllegalArgumentException(value);
      } else {
        return constant;
      }
    }
  }

  @Override
  @Generated("Eclipse")
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((expirationDate == null) ? 0 : expirationDate.hashCode());
    result = prime * result + ((pickupLocationId == null) ? 0 : pickupLocationId.hashCode());
    result = prime * result + ((item == null) ? 0 : item.hashCode());
    result = prime * result + queuePosition;
    result = prime * result + ((requestDate == null) ? 0 : requestDate.hashCode());
    result = prime * result + ((requestId == null) ? 0 : requestId.hashCode());
    result = prime * result + ((status == null) ? 0 : status.hashCode());
    return result;
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
    if (!(obj instanceof Hold)) {
      return false;
    }
    Hold other = (Hold) obj;
    if (expirationDate == null) {
      if (other.expirationDate != null) {
        return false;
      }
    } else if (!expirationDate.equals(other.expirationDate)) {
      return false;
    }
    if (pickupLocationId == null) {
      if (other.pickupLocationId != null) {
        return false;
      }
    } else if (!pickupLocationId.equals(other.pickupLocationId)) {
      return false;
    }
    if (item == null) {
      if (other.item != null) {
        return false;
      }
    } else if (!item.equals(other.item)) {
      return false;
    }
    if (queuePosition != other.queuePosition) {
      return false;
    }
    if (requestDate == null) {
      if (other.requestDate != null) {
        return false;
      }
    } else if (!requestDate.equals(other.requestDate)) {
      return false;
    }
    if (requestId == null) {
      if (other.requestId != null) {
        return false;
      }
    } else if (!requestId.equals(other.requestId)) {
      return false;
    }
    if (status != other.status) {
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

  public static Hold fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, Hold.class);
  }

  public static Hold fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, Hold.class);
  }
}