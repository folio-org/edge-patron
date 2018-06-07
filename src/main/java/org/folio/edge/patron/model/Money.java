package org.folio.edge.patron.model;

import java.io.IOException;

import javax.annotation.Generated;

import org.folio.edge.core.utils.Mappers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "currency")
public final class Money {

  @JsonProperty("amount")
  public final float amount;

  @JsonProperty("isoCurrencyCode")
  public final String isoCurrencyCode;

  public Money(@JsonProperty("amount") float amount, @JsonProperty("isoCurrencyCode") String isoCurrencyCode) {
    this.amount = amount;
    this.isoCurrencyCode = isoCurrencyCode;
  }

  @Override
  @Generated("Eclipse")
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) amount;
    result = prime * result + ((isoCurrencyCode == null) ? 0 : isoCurrencyCode.hashCode());
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
    if (!(obj instanceof Money)) {
      return false;
    }
    Money other = (Money) obj;
    if (Float.compare(amount, other.amount) != 0) {
      return false;
    }
    if (isoCurrencyCode == null) {
      if (other.isoCurrencyCode != null) {
        return false;
      }
    } else if (!isoCurrencyCode.equals(other.isoCurrencyCode)) {
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

  public static Money fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, Money.class);
  }

  public static Money fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, Money.class);
  }
}
