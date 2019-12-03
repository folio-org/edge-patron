package org.folio.edge.patron.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.apache.log4j.Logger;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.loader.SchemaLoader;
import org.folio.edge.core.utils.Mappers;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HoldCancellationTest {

  private static final Logger logger = Logger.getLogger(HoldCancellationTest.class);
  private static final String SCHEMA = "ramls/hold-cancellation.json";
  private static final String XSD = "ramls/patron.xsd";

  private org.everit.json.schema.Schema jsonValidator;
  private Validator xmlValidator;

  private HoldCancellation holdCancellation;

  @Before
  public void setUp() throws Exception {

    long holdCanceledTs = System.currentTimeMillis();

    holdCancellation = HoldCancellation.builder()
      .holdId(UUID.randomUUID().toString())
      .canceledByUserId(UUID.randomUUID().toString())
      .canceledDate(new Date(holdCanceledTs))
      .cancellationReasonId(UUID.randomUUID().toString())
      .build();

    SchemaFactory schemaFactory = SchemaFactory
      .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new File(XSD));
    xmlValidator = schema.newValidator();

    FormatValidator formatValidator = dateTime -> {
      try {
        new SimpleDateFormat(Mappers.DATE_FORMAT).parse(dateTime);
        return Optional.empty();
      } catch (Exception e) {
        return Optional.of(e.getMessage());
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
    String json = holdCancellation.toJson();
    logger.info("JSON: " + json);

    jsonValidator.validate(new JSONObject(new JSONTokener(json)));

    HoldCancellation fromJson = HoldCancellation.fromJson(json);
    assertEquals(holdCancellation, fromJson);
  }

  @Test
  public void testToFromXml() throws IOException {
    String xml = holdCancellation.toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    try {
      xmlValidator.validate(source);
    } catch (SAXException e) {
      fail("XML validation failed: " + e.getMessage());
    }

    HoldCancellation fromXml = HoldCancellation.fromXml(xml);
    assertEquals(holdCancellation, fromXml);
  }

  @Test
  public void testJsonToXml() throws IOException {
    String json = holdCancellation.toJson();
    HoldCancellation fromJson = HoldCancellation.fromJson(json);
    String xml = fromJson.toXml();
    HoldCancellation fromXml = HoldCancellation.fromXml(xml);

    logger.info(json);
    logger.info(xml);

    assertEquals(holdCancellation, fromJson);
    assertEquals(holdCancellation, fromXml);
  }

  @Test(expected = SAXException.class)
  public void testEmpty() throws Exception {
    String xml = HoldCancellation.builder().build().toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    xmlValidator.validate(source);
  }
}
