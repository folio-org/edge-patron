package org.folio.edge.patron.model;

import static org.folio.edge.core.Constants.DAY_IN_MILLIS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.loader.SchemaLoader;
import org.folio.edge.core.utils.Mappers;
import org.folio.edge.patron.model.Hold.FulfillmentPreference;
import org.folio.edge.patron.model.Hold.Status;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.jqno.equalsverifier.EqualsVerifier;

public class HoldTest {

  private static final Logger logger = Logger.getLogger(HoldTest.class);
  private static final String SCHEMA = "ramls/hold.json";
  private static final String XSD = "ramls/patron.xsd";

  private org.everit.json.schema.Schema jsonValidator;
  private Validator xmlValidator;

  private Hold hold;

  @Before
  public void setUp() throws Exception {
    Item item = Item.builder()
      .author("Priest, Christopher")
      .title("The Inverted World")
      .instanceId(UUID.randomUUID().toString())
      .itemId(UUID.randomUUID().toString())
      .isbn("0008675309")
      .build();

    long holdExpTs = System.currentTimeMillis() + (59 * DAY_IN_MILLIS);
    long holdReqTs = System.currentTimeMillis() - DAY_IN_MILLIS;

    hold = Hold.builder()
      .item(item)
      .fulfillmentPreference(FulfillmentPreference.HOLD_SHELF)
      .expirationDate(new SimpleDateFormat(Hold.DATE_FORMAT).format(new Date(holdExpTs)))
      .queueLength(3)
      .queuePosition(2)
      .requestDate(new Date(holdReqTs))
      .requestId(UUID.randomUUID().toString())
      .status(Status.OPEN_NOT_YET_FILLED)
      .build();

    SchemaFactory schemaFactory = SchemaFactory
      .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new File(XSD));
    xmlValidator = schema.newValidator();

    FormatValidator formatValidator = new FormatValidator() {

      @Override
      public Optional<String> validate(String dateTime) {
        try {
          new SimpleDateFormat(Mappers.DATE_FORMAT).parse(dateTime);
          return Optional.empty();
        } catch (Exception e) {
          return Optional.of(e.getMessage());
        }
      }
    };

    JSONObject schemaJson = new JSONObject(new JSONTokener(new FileInputStream(new File(SCHEMA))));
    SchemaLoader schemaLoader = SchemaLoader.builder()
      .schemaJson(schemaJson)
      .addFormatValidator("date-time", formatValidator)
      .build();
    jsonValidator = schemaLoader.load().build();
  }

  @Test
  public void testEqualsContract() {
    EqualsVerifier.forClass(Hold.class).verify();
  }

  @Test
  public void testToFromJson() throws IOException {
    String json = hold.toJson();
    logger.info("JSON: " + json);

    jsonValidator.validate(new JSONObject(new JSONTokener(json)));

    Hold fromJson = Hold.fromJson(json);
    assertEquals(hold, fromJson);
  }

  @Test
  public void testToFromXml() throws IOException {
    String xml = hold.toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    try {
      xmlValidator.validate(source);
    } catch (SAXException e) {
      fail("XML validation failed: " + e.getMessage());
    }

    Hold fromXml = Hold.fromXml(xml);
    assertEquals(hold, fromXml);
  }

  @Test
  public void testJsonToXml() throws IOException {
    String json = hold.toJson();
    Hold fromJson = Hold.fromJson(json);
    String xml = fromJson.toXml();
    Hold fromXml = Hold.fromXml(xml);

    logger.info(json);
    logger.info(xml);

    assertEquals(hold, fromJson);
    assertEquals(hold, fromXml);
  }

  @Test(expected = SAXException.class)
  public void testEmpty() throws Exception {
    String xml = Hold.builder().build().toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    xmlValidator.validate(source);
  }

}
