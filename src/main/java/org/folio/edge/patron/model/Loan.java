package org.folio.edge.patron.model;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

import org.folio.edge.core.utils.Mappers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "loan")
@JsonDeserialize(builder = Loan.Builder.class)
public final class Loan {

  public final Item item;
  public final Date loanDate;
  public final Date dueDate;
  public final boolean overdue;

  private Loan(Item item, Date loanDate, Date dueDate, boolean overdue) {
    this.item = item;
    this.loanDate = loanDate;
    this.dueDate = dueDate;
    this.overdue = overdue;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    @JsonProperty("item")
    public Item item;

    @JsonProperty("loanDate")
    public Date loanDate;

    @JsonProperty("dueDate")
    public Date dueDate;

    @JsonProperty("overdue")
    public boolean overdue;

    public Builder item(Item item) {
      this.item = item;
      return this;
    }

    public Builder loanDate(Date loanDate) {
      this.loanDate = loanDate;
      return this;
    }

    public Builder dueDate(Date dueDate) {
      this.dueDate = dueDate;
      return this;
    }

    public Builder overdue(boolean overdue) {
      this.overdue = overdue;
      return this;
    }

    public Loan build() {
      return new Loan(item, loanDate, dueDate, overdue);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((loanDate == null) ? 0 : loanDate.hashCode());
    result = prime * result + ((dueDate == null) ? 0 : dueDate.hashCode());
    result = prime * result + ((item == null) ? 0 : item.hashCode());
    result = prime * result + (overdue ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Loan loan = (Loan) o;
    return overdue == loan.overdue &&
      Objects.equals(item, loan.item) &&
      Objects.equals(loanDate, loan.loanDate) &&
      Objects.equals(dueDate, loan.dueDate);
  }

  public String toXml() throws JsonProcessingException {
    return Mappers.XML_PROLOG + Mappers.xmlMapper.writeValueAsString(this);
  }

  public String toJson() throws JsonProcessingException {
    return Mappers.jsonMapper.writeValueAsString(this);
  }

  public static Loan fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, Loan.class);
  }

  public static Loan fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, Loan.class);
  }
}
