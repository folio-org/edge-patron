package org.folio.edge.patron.model;

import static org.folio.edge.core.Constants.DAY_IN_MILLIS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.loader.SchemaLoader;
import org.folio.edge.core.utils.Mappers;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ChargeTest {

  private static final Logger logger = LogManager.getLogger(ChargeTest.class);
  private static final String SCHEMA = "ramls/charge.json";
  private static final String XSD = "ramls/patron.xsd";

  private org.everit.json.schema.Schema jsonValidator;
  private Validator xmlValidator;

  private Charge charge;

  @Before
  public void setUp() throws Exception {
    Item item = Item.builder()
      .author("Priest, Christopher")
      .title("The Inverted World")
      .instanceId(UUID.randomUUID().toString())
      .itemId(UUID.randomUUID().toString())
      .isbn("0008675309")
      .build();

    long checkedOutTs = System.currentTimeMillis() - (59 * DAY_IN_MILLIS);
    long dueTs = checkedOutTs + (14 * DAY_IN_MILLIS);
    long accrualTs = dueTs + (7 * DAY_IN_MILLIS);

    charge = Charge.builder()
      .item(item)
      .feeFineId(UUID.randomUUID().toString())
      .accrualDate(new Date(accrualTs))
      .chargeAmount(new Money(1.23f, Currency.getInstance("USD").getCurrencyCode()))
      .description("late fee")
      .reason("item overdue")
      .state("outstanding")
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
    EqualsVerifier.forClass(Charge.class).verify();
  }

  @Test
  public void testToFromJson() throws IOException {
    String json = charge.toJson();
    logger.info("JSON: " + json);

    jsonValidator.validate(new JSONObject(new JSONTokener(json)));

    Charge fromJson = Charge.fromJson(json);
    assertEquals(charge, fromJson);
  }

  @Test
  public void testToFromXml() throws IOException {
    String xml = charge.toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    try {
      xmlValidator.validate(source);
    } catch (SAXException e) {
      fail("XML validation failed: " + e.getMessage());
    }

    Charge fromXml = Charge.fromXml(xml);
    assertEquals(charge, fromXml);
  }

  @Test
  public void testJsonToXml() throws IOException {
    String json = charge.toJson();
    Charge fromJson = Charge.fromJson(json);
    String xml = fromJson.toXml();
    Charge fromXml = Charge.fromXml(xml);

    logger.info(json);
    logger.info(xml);

    assertEquals(charge, fromJson);
    assertEquals(charge, fromXml);
  }

  @Test(expected = SAXException.class)
  public void testEmpty() throws Exception {
    String xml = Charge.builder().build().toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    xmlValidator.validate(source);
  }

}
