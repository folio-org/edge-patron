
package org.folio.edge.patron.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import org.folio.edge.core.utils.Mappers;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "itemsPendingDetail")
@JsonPropertyOrder({
    "instanceId",
    "itemId",
    "title",
    "pickUpLocationId"
})
public class ItemsPendingDetail extends BaseItemsDetail {

  public ItemsPendingDetail() {}

  public ItemsPendingDetail(String instanceId, String itemId, String title, String pickUpLocationId) {
    super(instanceId, itemId, title, pickUpLocationId);
  }

  public String toXml() throws JsonProcessingException {
    return Mappers.XML_PROLOG + Mappers.xmlMapper.writeValueAsString(this);
  }

  public String toJson() throws JsonProcessingException {
    return Mappers.jsonMapper.writeValueAsString(this);
  }

  public static ItemsPendingDetail fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, ItemsPendingDetail.class);
  }

  public static ItemsPendingDetail fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, ItemsPendingDetail.class);
  }
}
