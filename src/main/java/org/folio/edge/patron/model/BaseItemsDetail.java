package org.folio.edge.patron.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Objects;


public class BaseItemsDetail {
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

  public BaseItemsDetail() {}

  public BaseItemsDetail(String instanceId, String itemId, String title, String pickUpLocationId) {
    this.instanceId = instanceId;
    this.itemId = itemId;
    this.title = title;
    this.pickUpLocationId = pickUpLocationId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BaseItemsDetail that = (BaseItemsDetail) o;
    return Objects.equals(instanceId, that.instanceId) && Objects.equals(itemId, that.itemId) && Objects.equals(title, that.title) && Objects.equals(pickUpLocationId, that.pickUpLocationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instanceId, itemId, title, pickUpLocationId);
  }
}
