package org.folio.edge.patron.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.folio.edge.core.utils.Mappers;

public class Patron {
  @JsonProperty("id")
  public final String id;

  public Patron(String id) {
    this.id = id;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String toJson() throws JsonProcessingException {
    return Mappers.jsonMapper.writeValueAsString(this);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Builder {
    private String id;

    @JsonProperty("id")
    public Patron.Builder id(String id) {
      this.id = id;
      return this;
    }

    public Patron build() {
      return new Patron(id);
    }
  }
  //TODO
}
