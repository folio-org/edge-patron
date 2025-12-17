
package org.folio.edge.patron.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import java.util.Objects;
import org.folio.edge.core.utils.Mappers;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "itemsFailedDetail")
@JsonPropertyOrder({
    "instanceId",
    "itemId",
    "title",
    "pickUpLocationId",
    "errorCode",
    "errorDetails"
})
public class ItemsFailedDetail {
  /**
   * The UUID id of the instance
   *
   */
  @JsonProperty("instanceId")
  @JsonPropertyDescription("The UUID id of the instance")
  private String instanceId;
  /**
   * UUID of the item
   *
   */
  @JsonProperty("itemId")
  @JsonPropertyDescription("UUID of the item")
  private String itemId;
  /**
   * Title of the item
   *
   */
  @JsonProperty("title")
  @JsonPropertyDescription("Title of the item")
  private String title;
  /**
   * UUID of the pickup location
   *
   */
  @JsonProperty("pickUpLocationId")
  @JsonPropertyDescription("UUID of the pickup location")
  private String pickUpLocationId;
  /**
   * Error code indicating the reason for failure
   *
   */
  @JsonProperty("errorCode")
  @JsonPropertyDescription("Error code indicating the reason for failure")
  private String errorCode;
  /**
   * Detailed message describing the failure
   *
   */
  @JsonProperty("errorDetails")
  @JsonPropertyDescription("Detailed message describing the failure")
  private String errorDetails;

  public ItemsFailedDetail() {}

  public ItemsFailedDetail(String instanceId, String itemId, String title, String pickUpLocationId, String errorCode, String errorDetails) {
    this.instanceId = instanceId;
    this.itemId = itemId;
    this.title = title;
    this.pickUpLocationId = pickUpLocationId;
    this.errorCode = errorCode;
    this.errorDetails = errorDetails;
  }



  /**
   * The UUID id of the instance
   *
   */
  @JsonProperty("instanceId")
  public String getInstanceId() {
      return instanceId;
  }

  /**
   * UUID of the item
   *
   */
  @JsonProperty("itemId")
  public String getItemId() {
      return itemId;
  }

  /**
   * Title of the item
   *
   */
  @JsonProperty("title")
  public String getTitle() {
      return title;
  }

  /**
   * UUID of the pickup location
   *
   */
  @JsonProperty("pickUpLocationId")
  public String getPickUpLocationId() {
      return pickUpLocationId;
  }

  /**
   * Error code indicating the reason for failure
   *
   */
  @JsonProperty("errorCode")
  public String getErrorCode() {
      return errorCode;
  }

  /**
   * Detailed message describing the failure
   *
   */
  @JsonProperty("errorDetails")
  public String getErrorDetails() {
      return errorDetails;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ItemsFailedDetail that = (ItemsFailedDetail) o;
    return Objects.equals(instanceId, that.instanceId) && Objects.equals(itemId, that.itemId) && Objects.equals(title, that.title) && Objects.equals(pickUpLocationId, that.pickUpLocationId) && Objects.equals(errorCode, that.errorCode) && Objects.equals(errorDetails, that.errorDetails);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instanceId, itemId, title, pickUpLocationId, errorCode, errorDetails);
  }

  public String toXml() throws JsonProcessingException {
    return Mappers.XML_PROLOG + Mappers.xmlMapper.writeValueAsString(this);
  }

  public String toJson() throws JsonProcessingException {
    return Mappers.jsonMapper.writeValueAsString(this);
  }

  public static ItemsFailedDetail fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, ItemsFailedDetail.class);
  }

  public static ItemsFailedDetail fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, ItemsFailedDetail.class);
  }
}
