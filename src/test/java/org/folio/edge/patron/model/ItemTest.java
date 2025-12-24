package org.folio.edge.patron.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import nl.jqno.equalsverifier.EqualsVerifier;

class ItemTest {

  private static final Logger logger = Logger.getLogger(ItemTest.class);
  private static final String SCHEMA = "ramls/item.json";
  private static final String XSD = "ramls/patron.xsd";

  private org.everit.json.schema.Schema jsonValidator;
  private Validator xmlValidator;

  private Item item;

  @BeforeEach
  void setUp() throws Exception {
    item = Item.builder()
      .author("Priest, Christopher")
      .title("The Inverted World")
      .instanceId(UUID.randomUUID().toString())
      .itemId(UUID.randomUUID().toString())
      .isbn("0008675309")
      .build();

    SchemaFactory schemaFactory = SchemaFactory
      .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new File(XSD));
    xmlValidator = schema.newValidator();

    JSONObject schemaJson = new JSONObject(new JSONTokener(new FileInputStream(SCHEMA)));
    jsonValidator = SchemaLoader.load(schemaJson);
  }

  @Test
  void testEqualsContract() {
    EqualsVerifier.forClass(Item.class).verify();
  }

  @Test
  void testToFromJson() throws IOException {
    String json = item.toJson();
    logger.info("JSON: " + json);

    jsonValidator.validate(new JSONObject(new JSONTokener(json)));

    Item fromJson = Item.fromJson(json);
    assertEquals(item, fromJson);
  }

  @Test
  void testToFromXml() throws IOException {
    String xml = item.toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    try {
      xmlValidator.validate(source);
    } catch (SAXException e) {
      fail("XML validation failed: " + e.getMessage());
    }

    Item fromXml = Item.fromXml(xml);
    assertEquals(item, fromXml);
  }

  @Test
  void testJsonToXml() throws IOException {
    String json = item.toJson();
    Item fromJson = Item.fromJson(json);
    String xml = fromJson.toXml();
    Item fromXml = Item.fromXml(xml);

    logger.info(json);
    logger.info(xml);

    assertEquals(item, fromJson);
    assertEquals(item, fromXml);
  }

  @Test
  public void testEmpty() throws Exception {
    String xml = Item.builder().build().toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    assertThrows(SAXException.class, () -> xmlValidator.validate(source));
  }

}
