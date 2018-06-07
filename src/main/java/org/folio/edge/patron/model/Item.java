package org.folio.edge.patron.model;

import java.io.IOException;

import javax.annotation.Generated;

import org.folio.edge.core.utils.Mappers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "item")
@JsonDeserialize(builder = Item.Builder.class)
@JsonPropertyOrder({
    "title",
    "author",
    "instanceId",
    "itemId",
    "isbn"
})
public final class Item {

  public final String instanceId;
  public final String itemId;
  public final String title;
  public final String author;
  public final String isbn;

  private Item(String instanceId, String itemId, String title, String author, String isbn) {
    this.instanceId = instanceId;
    this.itemId = itemId;
    this.title = title;
    this.author = author;
    this.isbn = isbn;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    @JsonProperty("instanceId")
    private String instanceId;

    @JsonProperty("itemId")
    private String itemId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("author")
    private String author;

    @JsonProperty("isbn")
    private String isbn;

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder itemId(String itemId) {
      this.itemId = itemId;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder author(String author) {
      this.author = author;
      return this;
    }

    public Builder isbn(String isbn) {
      this.isbn = isbn;
      return this;
    }

    public Item build() {
      return new Item(instanceId, itemId, title, author, isbn);
    }
  }

  @Override
  @Generated("Eclipse")
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((author == null) ? 0 : author.hashCode());
    result = prime * result + ((title == null) ? 0 : title.hashCode());
    result = prime * result + ((instanceId == null) ? 0 : instanceId.hashCode());
    result = prime * result + ((itemId == null) ? 0 : itemId.hashCode());
    result = prime * result + ((isbn == null) ? 0 : isbn.hashCode());
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
    if (!(obj instanceof Item)) {
      return false;
    }
    Item other = (Item) obj;
    if (author == null) {
      if (other.author != null) {
        return false;
      }
    } else if (!author.equals(other.author)) {
      return false;
    }
    if (title == null) {
      if (other.title != null) {
        return false;
      }
    } else if (!title.equals(other.title)) {
      return false;
    }
    if (instanceId == null) {
      if (other.instanceId != null) {
        return false;
      }
    } else if (!instanceId.equals(other.instanceId)) {
      return false;
    }
    if (itemId == null) {
      if (other.itemId != null) {
        return false;
      }
    } else if (!itemId.equals(other.itemId)) {
      return false;
    }
    if (isbn == null) {
      if (other.isbn != null) {
        return false;
      }
    } else if (!isbn.equals(other.isbn)) {
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

  public static Item fromJson(String json) throws IOException {
    return Mappers.jsonMapper.readValue(json, Item.class);
  }

  public static Item fromXml(String xml) throws IOException {
    return Mappers.xmlMapper.readValue(xml, Item.class);
  }
}