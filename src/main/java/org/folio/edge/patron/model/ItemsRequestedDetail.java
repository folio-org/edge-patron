
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
@JacksonXmlRootElement(localName = "itemsRequestedDetail")
@JsonPropertyOrder({
    "instanceId",
    "itemId",
    "title",
    "pickUpLocationId",
    "confirmedRequestId"
})
public class ItemsRequestedDetail {

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
   * The UUID id of created item request
   *
   */
  @JsonProperty("confirmedRequestId")
  @JsonPropertyDescription("The UUID id of created item request")
  private String confirmedRequestId;

  public ItemsRequestedDetail() {}

  public ItemsRequestedDetail(String instanceId, String itemId, String title, String pickUpLocationId, String confirmedRequestId) {
    this.instanceId = instanceId;
    this.itemId = itemId;
    this.title = title;
    this.pickUpLocationId = pickUpLocationId;
    this.confirmedRequestId = confirmedRequestId;
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
   * The UUID id of created item request
   *
   */
  @JsonProperty("confirmedRequestId")
  public String getConfirmedRequestId() {
      return confirmedRequestId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ItemsRequestedDetail that = (ItemsRequestedDetail) o;
    return Objects.equals(instanceId, that.instanceId) && Objects.equals(itemId, that.itemId) && Objects.equals(title, that.title) && Objects.equals(pickUpLocationId, that.pickUpLocationId) && Objects.equals(confirmedRequestId, that.confirmedRequestId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instanceId, itemId, title, pickUpLocationId, confirmedRequestId);
  }

  public String toXml() throws JsonProcessingException {
    return Mappers.XML_PROLOG + Mappers.xmlMapper.writeValueAsString(this);
  }

  public String toJson() throws JsonProcessingException {
    return Mappers.jsonMapper.writeValueAsString(this);
  }

  public static ItemsRequestedDetail fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, ItemsRequestedDetail.class);
  }

  public static ItemsRequestedDetail fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, ItemsRequestedDetail.class);
  }
}
