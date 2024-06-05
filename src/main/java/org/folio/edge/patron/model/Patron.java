package org.folio.edge.patron.model;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.folio.edge.core.utils.Mappers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "patron")
@JsonDeserialize(builder = Patron.Builder.class)
@JsonPropertyOrder({
  "generalInfo",
  "address0",
  "address1",
  "contactInfo",
  "preferredEmailCommunication"
})
public final class Patron {
  public final GeneralInfo generalInfo;
  public final Address address0;
  public final Address address1;
  public final ContactInfo contactInfo;
  public final List<String> preferredEmailCommunication;

  private Patron(Builder builder) {
    this.generalInfo = builder.generalInfo;
    this.address0 = builder.address0;
    this.address1 = builder.address1;
    this.contactInfo = builder.contactInfo;
    this.preferredEmailCommunication = builder.preferredEmailCommunication;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    @JsonProperty("generalInfo")
    private GeneralInfo generalInfo;

    @JsonProperty("address0")
    private Address address0;

    @JsonProperty("address1")
    private Address address1;

    @JsonProperty("contactInfo")
    private ContactInfo contactInfo;

    @JsonProperty("preferredEmailCommunication")
    private List<String> preferredEmailCommunication;

    public Builder generalInfo(GeneralInfo generalInfo) {
      this.generalInfo = generalInfo;
      return this;
    }

    public Builder address0(Address address0) {
      this.address0 = address0;
      return this;
    }

    public Builder address1(Address address1) {
      this.address1 = address1;
      return this;
    }

    public Builder contactInfo(ContactInfo contactInfo) {
      this.contactInfo = contactInfo;
      return this;
    }

    public Builder preferredEmailCommunication(List<String> preferredEmailCommunication) {
      this.preferredEmailCommunication = preferredEmailCommunication;
      return this;
    }

    public Patron build() {
      return new Patron(this);
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonPropertyOrder({
    "externalSystemId",
    "firstName",
    "preferredFirstName",
    "middleName",
    "lastName"
  })
  public static class GeneralInfo {
    public final String externalSystemId;
    public final String firstName;
    public final String preferredFirstName;
    public final String middleName;
    public final String lastName;

    @JsonCreator
    public GeneralInfo(
      @JsonProperty("externalSystemId") String externalSystemId,
      @JsonProperty("firstName") String firstName,
      @JsonProperty("preferredFirstName") String preferredFirstName,
      @JsonProperty("middleName") String middleName,
      @JsonProperty("lastName") String lastName) {
      this.externalSystemId = externalSystemId;
      this.firstName = firstName;
      this.preferredFirstName = preferredFirstName;
      this.middleName = middleName;
      this.lastName = lastName;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonPropertyOrder({
    "addressLine0",
    "addressLine1",
    "city",
    "province",
    "zip",
    "country"
  })
  public static class Address {
    public final String addressLine0;
    public final String addressLine1;
    public final String city;
    public final String province;
    public final String zip;
    public final String country;

    @JsonCreator
    public Address(
      @JsonProperty("addressLine0") String addressLine0,
      @JsonProperty("addressLine1") String addressLine1,
      @JsonProperty("city") String city,
      @JsonProperty("province") String province,
      @JsonProperty("zip") String zip,
      @JsonProperty("country") String country) {
      this.addressLine0 = addressLine0;
      this.addressLine1 = addressLine1;
      this.city = city;
      this.province = province;
      this.zip = zip;
      this.country = country;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonPropertyOrder({
    "phone",
    "mobilePhone",
    "email"
  })
  public static class ContactInfo {
    public final String phone;
    public final String mobilePhone;
    public final String email;

    @JsonCreator
    public ContactInfo(
      @JsonProperty("phone") String phone,
      @JsonProperty("mobilePhone") String mobilePhone,
      @JsonProperty("email") String email) {
      this.phone = phone;
      this.mobilePhone = mobilePhone;
      this.email = email;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(generalInfo, address0, address1, contactInfo, preferredEmailCommunication);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Patron patron = (Patron) o;
    return Objects.equals(generalInfo, patron.generalInfo) &&
      Objects.equals(address0, patron.address0) &&
      Objects.equals(address1, patron.address1) &&
      Objects.equals(contactInfo, patron.contactInfo) &&
      Objects.equals(preferredEmailCommunication, patron.preferredEmailCommunication);
  }

  public String toXml() throws JsonProcessingException {
    return Mappers.XML_PROLOG + Mappers.xmlMapper.writeValueAsString(this);
  }

  public String toJson() throws JsonProcessingException {
    return Mappers.jsonMapper.writeValueAsString(this);
  }

  public static Patron fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, Patron.class);
  }

  public static Patron fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, Patron.class);
  }
}
