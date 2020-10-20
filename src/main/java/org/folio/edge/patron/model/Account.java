package org.folio.edge.patron.model;

import static org.folio.edge.patron.Constants.DEFAULT_CURRENCY_CODE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.folio.edge.core.utils.Mappers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "account")
@JsonDeserialize(builder = Account.Builder.class)
@JsonPropertyOrder({
    "id",
    "totalCharges",
    "totalChargesCount",
    "totalLoans",
    "totalHolds",
    "charges",
    "holds",
    "loans"
})
public final class Account {
  @JsonProperty("id")
  public final String id;

  @JsonProperty("totalCharges")
  public final Money totalCharges;

  @JsonProperty("totalChargesCount")
  public final int totalChargesCount;

  @JsonProperty("totalLoans")
  public final int totalLoans;

  @JsonProperty("totalHolds")
  public final int totalHolds;

  @JsonProperty("charges")
  @JacksonXmlProperty(localName = "charge")
  @JacksonXmlElementWrapper(localName = "charges")
  public final List<Charge> charges;

  @JsonProperty("holds")
  @JacksonXmlProperty(localName = "hold")
  @JacksonXmlElementWrapper(localName = "holds")
  public final List<Hold> holds;

  @JsonProperty("loans")
  @JacksonXmlProperty(localName = "loan")
  @JacksonXmlElementWrapper(localName = "loans")
  public final List<Loan> loans;

  // (internal) copy constructor to help with conditionally marshaled fields
  private Account(Account other, boolean includeCharges, boolean includeHolds, boolean includeLoans) {
    this.id = other.id;

    this.totalCharges = other.totalCharges;

    this.charges = includeCharges ? other.charges : new ArrayList<>(0);
    this.holds = includeHolds ? other.holds : new ArrayList<>(0);
    this.loans = includeLoans ? other.loans : new ArrayList<>(0);

    this.totalChargesCount = other.totalChargesCount;
    this.totalLoans = other.totalLoans;
    this.totalHolds = other.totalHolds;
  }

  private Account(String id, List<Charge> charges, List<Hold> holds, List<Loan> loans) {
    this.id = id;

    Money tc = null;
    if (charges != null) {
      for (Charge c : charges) {
        tc = tc == null ? c.chargeAmount
            : new Money(tc.amount + c.chargeAmount.amount, c.chargeAmount.isoCurrencyCode);
      }
    } else {
      tc = new Money(0f, DEFAULT_CURRENCY_CODE);
    }
    totalCharges = tc;

    if (charges == null) {
      charges = new ArrayList<>(0);
    }

    if (holds == null) {
      holds = new ArrayList<>(0);
    }

    if (loans == null) {
      loans = new ArrayList<>(0);
    }

    this.totalChargesCount = charges.size();
    this.totalLoans = loans.size();
    this.totalHolds = holds.size();
    this.charges = charges;
    this.holds = holds;
    this.loans = loans;
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Builder {
    private String id;
    private List<Charge> charges;
    private List<Hold> holds;
    private List<Loan> loans;

    @JsonProperty("id")
    public Builder id(String id) {
      this.id = id;
      return this;
    }

    @JsonProperty("charges")
    public Builder charges(List<Charge> charges) {
      this.charges = charges;
      return this;
    }

    @JsonProperty("holds")
    public Builder holds(List<Hold> holds) {
      this.holds = holds;
      return this;
    }

    @JsonProperty("loans")
    public Builder loans(List<Loan> loans) {
      this.loans = loans;
      return this;
    }

    public Account build() {
      return new Account(id, charges, holds, loans);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((charges == null) ? 0 : charges.hashCode());
    result = prime * result + ((loans == null) ? 0 : loans.hashCode());
    result = prime * result + ((holds == null) ? 0 : holds.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((totalCharges == null) ? 0 : totalCharges.hashCode());
    result = prime * result + totalChargesCount;
    result = prime * result + totalLoans;
    result = prime * result + totalHolds;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Account account = (Account) o;
    return totalChargesCount == account.totalChargesCount &&
      totalLoans == account.totalLoans &&
      totalHolds == account.totalHolds &&
      Objects.equals(id, account.id) &&
      Objects.equals(totalCharges, account.totalCharges) &&
      Objects.equals(charges, account.charges) &&
      Objects.equals(holds, account.holds) &&
      Objects.equals(loans, account.loans);
  }

  public String toXml(boolean includeLoans, boolean includeCharges, boolean includeHolds)
      throws JsonProcessingException {
    if (includeCharges && includeHolds && includeLoans) {
      return toXml();
    } else {
      return new Account(this, includeCharges, includeHolds, includeLoans).toXml();
    }
  }

  public String toXml() throws JsonProcessingException {
    return Mappers.XML_PROLOG + Mappers.xmlMapper.writeValueAsString(this);
  }

  public String toJson(boolean includeLoans, boolean includeCharges, boolean includeHolds)
      throws JsonProcessingException {
    if (includeCharges && includeHolds && includeLoans) {
      return toJson();
    } else {
      return new Account(this, includeCharges, includeHolds, includeLoans).toJson();
    }
  }

  public String toJson() throws JsonProcessingException {
    return Mappers.jsonMapper.writeValueAsString(this);
  }

  public static Account fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, Account.class);
  }

  public static Account fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, Account.class);
  }
}
