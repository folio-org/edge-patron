package org.folio.edge.patron.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Currency;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.jqno.equalsverifier.EqualsVerifier;

public class MoneyTest {

  private static final Logger logger = LogManager.getLogger(MoneyTest.class);
  private static final String SCHEMA = "ramls/money.json";
  private static final String XSD = "ramls/patron.xsd";

  private org.everit.json.schema.Schema jsonValidator;
  private Validator xmlValidator;

  private Money money;

  @Before
  public void setUp() throws Exception {
    money = new Money(1.23f, Currency.getInstance("USD").getCurrencyCode());

    SchemaFactory schemaFactory = SchemaFactory
      .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new File(XSD));
    xmlValidator = schema.newValidator();

    JSONObject schemaJson = new JSONObject(new JSONTokener(new FileInputStream(new File(SCHEMA))));
    jsonValidator = SchemaLoader.load(schemaJson);
  }

  @Test
  public void testEqualsContract() {
    EqualsVerifier.forClass(Money.class).verify();
  }

  @Test
  public void testToFromJson() throws IOException {
    String json = money.toJson();
    logger.info("JSON: " + json);

    jsonValidator.validate(new JSONObject(new JSONTokener(json)));

    Money fromJson = Money.fromJson(json);
    assertEquals(money, fromJson);
  }

  @Test
  public void testToFromXml() throws IOException {
    String xml = money.toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    try {
      xmlValidator.validate(source);
    } catch (SAXException e) {
      fail("XML validation failed: " + e.getMessage());
    }

    Money fromXml = Money.fromXml(xml);
    assertEquals(money, fromXml);
  }

  @Test
  public void testJsonToXml() throws IOException {
    String json = money.toJson();
    Money fromJson = Money.fromJson(json);
    String xml = fromJson.toXml();
    Money fromXml = Money.fromXml(xml);

    logger.info(json);
    logger.info(xml);

    assertEquals(money, fromJson);
    assertEquals(money, fromXml);
  }

  @Test
  public void testEmpty() throws Exception {
    String xml = new Money(0f, null).toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    try {
      xmlValidator.validate(source);
    } catch (Exception e) {
      fail("XML validation failed: " + e.getMessage());
    }
  }

}
