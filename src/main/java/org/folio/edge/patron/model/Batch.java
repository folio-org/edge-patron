
package org.folio.edge.patron.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.folio.edge.core.utils.Mappers;

/**
 * Multi-Item Request Processing Status
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "batch")
@JsonPropertyOrder({
    "batchRequestId",
    "status",
    "submittedAt",
    "completedAt",
    "itemsTotal",
    "itemsRequested",
    "itemsPending",
    "itemsFailed",
    "itemsPendingDetails",
    "itemsFailedDetails",
    "itemsRequestedDetails"
})
public class Batch {

  /**
   * The UUID id of the batch request
   * (Required)
   *
   */
  @JsonProperty("batchRequestId")
  @JsonPropertyDescription("The UUID id of the batch request")
  private String batchRequestId;

  /**
   * Status of the submitted batch request
   * (Required)
   *
   */
  @JsonProperty("status")
  @JsonPropertyDescription("Status of the submitted batch request")
  private Batch.Status status;

  /**
   * The date when the request was made
   * (Required)
   *
   */
  @JsonProperty("submittedAt")
  @JsonPropertyDescription("The date when the request was made")
  private  Date submittedAt;

  /**
   * The date when the request was completed
   *
   */
  @JsonProperty("completedAt")
  @JsonPropertyDescription("The date when the request was completed")
  private Date completedAt;
  /**
   * Total number of items to be requested in batch
   *
   */
  @JsonProperty("itemsTotal")
  @JsonPropertyDescription("Total number of items to be requested in batch")
  private Integer itemsTotal;

  /**
   * Number of items successfully requested
   *
   */
  @JsonProperty("itemsRequested")
  @JsonPropertyDescription("Number of items successfully requested")
  private Integer itemsRequested;

  /**
   * Number of items still pending to be requested
   *
   */
  @JsonProperty("itemsPending")
  @JsonPropertyDescription("Number of items still pending to be requested")
  private Integer itemsPending;
  /**
   * Number of items that failed to be requested
   *
   */
  @JsonProperty("itemsFailed")
  @JsonPropertyDescription("Number of items that failed to be requested")
  private Integer itemsFailed;

  /**
   * Collection of items request details that are still pending to be processed
   *
   */
  @JsonProperty("itemsPendingDetails")
  @JacksonXmlProperty(localName = "itemsPendingDetail")
  @JacksonXmlElementWrapper(localName = "itemsPendingDetails")
  @JsonPropertyDescription("Collection of items request details that are still pending to be processed")
  private List<ItemsPendingDetail> itemsPendingDetails;

  /**
   * Collection of items request details that are failed on processing and creating requests
   *
   */
  @JsonProperty("itemsFailedDetails")
  @JacksonXmlProperty(localName = "itemsFailedDetail")
  @JacksonXmlElementWrapper(localName = "itemsFailedDetails")
  @JsonPropertyDescription("Collection of items request details that are failed on processing and creating requests")
  private List<ItemsFailedDetail> itemsFailedDetails;

  /**
   * Collection of processed and created items requests details
   *
   */
  @JsonProperty("itemsRequestedDetails")
  @JacksonXmlProperty(localName = "itemsRequestedDetail")
  @JacksonXmlElementWrapper(localName = "itemsRequestedDetails")
  @JsonPropertyDescription("Collection of processed and created items requests details")
  private List<ItemsRequestedDetail> itemsRequestedDetails;

  public Batch withBatchRequestId(String batchRequestId) {
    this.batchRequestId = batchRequestId;
    return this;
  }

  public Batch withStatus(Batch.Status status) {
    this.status = status;
    return this;
  }

  public Batch withSubmittedAt(Date submittedAt) {
    this.submittedAt = submittedAt;
    return this;
  }

  public Batch withCompletedAt(Date completedAt) {
    this.completedAt = completedAt;
    return this;
  }

  public Batch withItemsTotal(Integer itemsTotal) {
    this.itemsTotal = itemsTotal;
    return this;
  }

  public Batch withItemsRequested(Integer itemsRequested) {
    this.itemsRequested = itemsRequested;
    return this;
  }

  public Batch withItemsPending(Integer itemsPending) {
    this.itemsPending = itemsPending;
    return this;
  }

  public Batch withItemsFailed(Integer itemsFailed) {
    this.itemsFailed = itemsFailed;
    return this;
  }

  public Batch withItemsPendingDetails(List<ItemsPendingDetail> itemsPendingDetails) {
    this.itemsPendingDetails = itemsPendingDetails;
    return this;
  }

  public Batch withItemsFailedDetails(List<ItemsFailedDetail> itemsFailedDetails) {
    this.itemsFailedDetails = itemsFailedDetails;
    return this;
  }

  public Batch withItemsRequestedDetails(List<ItemsRequestedDetail> itemsRequestedDetails) {
    this.itemsRequestedDetails = itemsRequestedDetails;
    return this;
  }

  public enum Status {

      IN_PROGRESS("In progress"),
      COMPLETED("Completed");
      private final String value;

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
        for (Status c: values()) {
          if (c.value.equals(value)) {
            return c;
          }
        }

        throw new IllegalArgumentException(value);
      }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Batch batch = (Batch) o;
    return Objects.equals(batchRequestId, batch.batchRequestId) && status == batch.status && Objects.equals(submittedAt, batch.submittedAt) && Objects.equals(completedAt, batch.completedAt) && Objects.equals(itemsTotal, batch.itemsTotal) && Objects.equals(itemsRequested, batch.itemsRequested) && Objects.equals(itemsPending, batch.itemsPending) && Objects.equals(itemsFailed, batch.itemsFailed) && Objects.equals(itemsPendingDetails, batch.itemsPendingDetails) && Objects.equals(itemsFailedDetails, batch.itemsFailedDetails) && Objects.equals(itemsRequestedDetails, batch.itemsRequestedDetails);
  }

  @Override
  public int hashCode() {
    return Objects.hash(batchRequestId, status, submittedAt, completedAt, itemsTotal, itemsRequested, itemsPending, itemsFailed, itemsPendingDetails, itemsFailedDetails, itemsRequestedDetails);
  }

  public String toXml() throws JsonProcessingException {
    return Mappers.XML_PROLOG + Mappers.xmlMapper.writeValueAsString(this);
  }

  public String toJson() throws JsonProcessingException {
    return Mappers.jsonMapper.writeValueAsString(this);
  }

  public static Batch fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, Batch.class);
  }

  public static Batch fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, Batch.class);
  }
}
