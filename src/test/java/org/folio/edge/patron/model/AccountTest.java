package org.folio.edge.patron.model;

import static org.folio.edge.core.Constants.DAY_IN_MILLIS;
import static org.folio.edge.patron.utils.PatronMockOkapi.getBatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.everit.json.schema.loader.SchemaLoader;
import org.folio.edge.patron.model.Hold.Status;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.jqno.equalsverifier.EqualsVerifier;

public class AccountTest {

  private static final Logger logger = Logger.getLogger(AccountTest.class);
  private static final String SCHEMA = "ramls/account.json";
  private static final String XSD = "ramls/patron.xsd";

  private org.everit.json.schema.Schema jsonValidator;
  private Validator xmlValidator;

  private Account account;

  @Before
  public void setUp() throws Exception {
    Item overdueCheckedOutItem1 = Item.builder()
      .author("Priest, Christopher")
      .title("The Inverted World")
      .instanceId(UUID.randomUUID().toString())
      .itemId(UUID.randomUUID().toString())
      .isbn("1590172698")
      .build();

    Item overdueCheckedOutItem2 = Item.builder()
      .author("Wyndham, John")
      .title("The Midwich Cuckoos")
      .instanceId(UUID.randomUUID().toString())
      .itemId(UUID.randomUUID().toString())
      .isbn("0141033010")
      .build();

    Item checkedOutItem = Item.builder()
      .author("Herbert, Frank")
      .title("Dune")
      .instanceId(UUID.randomUUID().toString())
      .itemId(UUID.randomUUID().toString())
      .isbn("0441172717")
      .build();

    Item holdItem = Item.builder()
      .author("Bradbury, Ray")
      .title("The Martian Chronicles")
      .instanceId(UUID.randomUUID().toString())
      .itemId(UUID.randomUUID().toString())
      .isbn("1451678193")
      .build();

    long checkedOutTs = System.currentTimeMillis() - (59 * DAY_IN_MILLIS);
    long dueTs = checkedOutTs + (14 * DAY_IN_MILLIS);
    long accrualTs = dueTs + (7 * DAY_IN_MILLIS);
    long holdExpTs = System.currentTimeMillis() + (60 * DAY_IN_MILLIS);
    long holdReqTs = System.currentTimeMillis();

    List<Charge> charges = new ArrayList<>();
    List<Hold> holds = new ArrayList<>();
    List<Loan> loans = new ArrayList<>();
    List<Batch> batches = new ArrayList<>();

    loans.add(Loan.builder()
      .item(overdueCheckedOutItem1)
      .overdue(true)
      .loanDate(new Date(checkedOutTs))
      .dueDate(new Date(dueTs))
      .build());

    loans.add(Loan.builder()
      .item(overdueCheckedOutItem2)
      .overdue(true)
      .loanDate(new Date(checkedOutTs + (2 * DAY_IN_MILLIS)))
      .dueDate(new Date(dueTs + (2 * DAY_IN_MILLIS)))
      .build());

    loans.add(Loan.builder()
      .item(checkedOutItem)
      .overdue(false)
      .loanDate(new Date(System.currentTimeMillis()))
      .dueDate(new Date(System.currentTimeMillis() + (14 * DAY_IN_MILLIS)))
      .build());

    holds.add(Hold.builder()
      .item(holdItem)
      .pickupLocationId(UUID.randomUUID().toString())
      .expirationDate(new Date(holdExpTs))
      .queuePosition(2)
      .requestDate(new Date(holdReqTs))
      .requestId(UUID.randomUUID().toString())
      .status(Status.OPEN_NOT_YET_FILLED)
      .build());

    charges.add(Charge.builder()
      .item(overdueCheckedOutItem1)
      .feeFineId(UUID.randomUUID().toString())
      .accrualDate(new Date(accrualTs))
      .chargeAmount(new Money(1.23f, Currency.getInstance("USD").getCurrencyCode()))
      .description("late fee")
      .reason("item overdue")
      .state("outstanding")
      .build());

    charges.add(Charge.builder()
      .item(overdueCheckedOutItem2)
      .feeFineId(UUID.randomUUID().toString())
      .accrualDate(new Date(accrualTs + (2 * DAY_IN_MILLIS)))
      .chargeAmount(new Money(1.12f, Currency.getInstance("USD").getCurrencyCode()))
      .description("late fee")
      .reason("item overdue")
      .state("outstanding")
      .build());

    batches.add(getBatch(UUID.randomUUID().toString()));

    account = Account.builder()
      .charges(charges)
      .holds(holds)
      .loans(loans)
      .batches(batches)
      .id(UUID.randomUUID().toString())
      .build();

    SchemaFactory schemaFactory = SchemaFactory
      .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new File(XSD));
    xmlValidator = schema.newValidator();

    JSONObject schemaJson = new JSONObject(new JSONTokener(new FileInputStream(new File(SCHEMA))));
    jsonValidator = SchemaLoader.load(schemaJson);
  }

  @Test
  public void testSumCharges() {
    float total = 0f;
    for (Charge c : account.charges) {
      total += c.chargeAmount.amount;
    }
    assertEquals(total, account.totalCharges.amount, 0.009f);
  }

  @Test
  public void testEqualsContract() {
    EqualsVerifier.forClass(Account.class).verify();
  }

  @Test
  public void testToFromJson() throws IOException {
    String json = account.toJson();
    logger.info("JSON: " + json);

    jsonValidator.validate(new JSONObject(new JSONTokener(json)));

    Account fromJson = Account.fromJson(json);
    assertEquals(account, fromJson);
  }

  @Test
  public void testToFromXml() throws IOException {
    String xml = account.toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    try {
      xmlValidator.validate(source);
    } catch (SAXException e) {
      fail("XML validation failed: " + e.getMessage());
    }

    Account fromXml = Account.fromXml(xml);
    assertEquals(account, fromXml);
  }

  @Test
  public void testJsonToXml() throws IOException {
    String json = account.toJson();
    Account fromJson = Account.fromJson(json);
    String xml = fromJson.toXml();
    Account fromXml = Account.fromXml(xml);

    logger.info(json);
    logger.info(xml);

    assertEquals(account, fromJson);
    assertEquals(account, fromXml);
  }

  @Test(expected = SAXException.class)
  public void testEmpty() throws Exception {
    String xml = Account.builder().build().toXml();
    logger.info("XML: " + xml);

    Source source = new StreamSource(new StringReader(xml));
    xmlValidator.validate(source);
  }

  @Test
  public void testNoLoans() throws Exception {
    String json = account.toJson(false, true, true, false);
    logger.info(json);
    assertTrue(Account.fromJson(json).loans.isEmpty());
    assertTrue(!Account.fromJson(json).charges.isEmpty());
    assertTrue(!Account.fromJson(json).holds.isEmpty());
  }

  @Test
  public void testNoCharges() throws Exception {
    String json = account.toJson(true, false, true, false);
    logger.info(json);
    assertTrue(!Account.fromJson(json).loans.isEmpty());
    assertTrue(Account.fromJson(json).charges.isEmpty());
    assertTrue(!Account.fromJson(json).holds.isEmpty());
  }

  @Test
  public void testNoHolds() throws Exception {
    String json = account.toJson(true, true, false, false);
    logger.info(json);
    assertTrue(!Account.fromJson(json).loans.isEmpty());
    assertTrue(!Account.fromJson(json).charges.isEmpty());
    assertTrue(Account.fromJson(json).holds.isEmpty());
  }

  @Test
  public void testCountOnly() throws Exception {
    String json = account.toJson(false, false, false, false);
    logger.info(json);
    assertTrue(Account.fromJson(json).loans.isEmpty());
    assertTrue(Account.fromJson(json).charges.isEmpty());
    assertTrue(Account.fromJson(json).holds.isEmpty());
  }
}
