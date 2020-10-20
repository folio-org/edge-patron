package org.folio.edge.patron.model;

import java.io.IOException;
import java.util.Date;

import org.folio.edge.core.utils.Mappers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "charge")
@JsonDeserialize(builder = Charge.Builder.class)
public final class Charge {

  public final Item item;
  public final Money chargeAmount;
  public final Date accrualDate;
  public final String description;
  public final String state;
  public final String reason;
  public final String feeFineId;

  private Charge(Item item, Money chargeAmount, Date accrualDate, String description, String state, String reason,
      String feeFineId) {
    this.item = item;
    this.chargeAmount = chargeAmount;
    this.accrualDate = accrualDate;
    this.description = description;
    this.state = state;
    this.reason = reason;
    this.feeFineId = feeFineId;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Item item;
    private Money chargeAmount;
    private Date accrualDate;
    private String description;
    private String state;
    private String reason;
    private String feeFineId;

    @JsonProperty("item")
    public Builder item(Item item) {
      this.item = item;
      return this;
    }

    @JsonProperty("chargeAmount")
    public Builder chargeAmount(Money chargeAmount) {
      this.chargeAmount = chargeAmount;
      return this;
    }

    @JsonProperty("accrualDate")
    public Builder accrualDate(Date accrualDate) {
      this.accrualDate = accrualDate;
      return this;
    }

    @JsonProperty("description")
    public Builder description(String description) {
      this.description = description;
      return this;
    }

    @JsonProperty("state")
    public Builder state(String state) {
      this.state = state;
      return this;
    }

    @JsonProperty("reason")
    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    @JsonProperty("feeFineId")
    public Builder feeFineId(String feeFineId) {
      this.feeFineId = feeFineId;
      return this;
    }

    public Charge build() {
      return new Charge(item, chargeAmount, accrualDate, description, state, reason, feeFineId);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((accrualDate == null) ? 0 : accrualDate.hashCode());
    result = prime * result + ((chargeAmount == null) ? 0 : chargeAmount.hashCode());
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((item == null) ? 0 : item.hashCode());
    result = prime * result + ((reason == null) ? 0 : reason.hashCode());
    result = prime * result + ((feeFineId == null) ? 0 : feeFineId.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
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
    if (!(obj instanceof Charge)) {
      return false;
    }
    Charge other = (Charge) obj;
    if (accrualDate == null) {
      if (other.accrualDate != null) {
        return false;
      }
    } else if (!accrualDate.equals(other.accrualDate)) {
      return false;
    }
    if (chargeAmount == null) {
      if (other.chargeAmount != null) {
        return false;
      }
    } else if (!chargeAmount.equals(other.chargeAmount)) {
      return false;
    }
    if (description == null) {
      if (other.description != null) {
        return false;
      }
    } else if (!description.equals(other.description)) {
      return false;
    }
    if (item == null) {
      if (other.item != null) {
        return false;
      }
    } else if (!item.equals(other.item)) {
      return false;
    }
    if (reason == null) {
      if (other.reason != null) {
        return false;
      }
    } else if (!reason.equals(other.reason)) {
      return false;
    }
    if (feeFineId == null) {
      if (other.feeFineId != null) {
        return false;
      }
    } else if (!feeFineId.equals(other.feeFineId)) {
      return false;
    }
    if (state == null) {
      if (other.state != null) {
        return false;
      }
    } else if (!state.equals(other.state)) {
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

  public static Charge fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, Charge.class);
  }

  public static Charge fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, Charge.class);
  }
}
