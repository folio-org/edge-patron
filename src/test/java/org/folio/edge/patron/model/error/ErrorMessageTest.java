package org.folio.edge.patron.model.error;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ErrorMessageTest {

    @Test
    public void TestErrorMessageEquals(){
        ErrorMessage messageOne = new ErrorMessage(400, "text", null);
        ErrorMessage messageTwo = new ErrorMessage(400, "text", null);

        assertEquals(messageOne, messageTwo);
        assertEquals(messageOne, messageOne);
    }

    @Test
    public void TestErrorMessageEquals1(){
      ErrorMessage errorMessage4 = new ErrorMessage(404, null, null);
      ErrorMessage errorMessage5 = new ErrorMessage(404, "Not Found", null);
      ErrorMessage errorMessage6 = new ErrorMessage(404, null, "ERR404");
      ErrorMessage errorMessage7 = new ErrorMessage(404, "Not Found", "ERR404");

      assertNotEquals(errorMessage4, errorMessage5);
      assertNotEquals(errorMessage4, errorMessage6);
      assertNotEquals(errorMessage5, errorMessage7);
      assertNotEquals(errorMessage6, errorMessage7);

      ErrorMessage errorMessage8 = new ErrorMessage(404, "Not Found", "ERR405");
      assertNotEquals(errorMessage7, errorMessage8);
    }

    @Test
    public void TestErrorMessageNotEquals(){

        ErrorMessage messageOne = new ErrorMessage(400, "text", null);
        assertNotEquals(messageOne, null);
        assertNotEquals(messageOne, "a string object");


        ErrorMessage messageTwo = new ErrorMessage(401, "text", null);
        assertNotEquals(messageOne, messageTwo);

        ErrorMessage messageThree  = new ErrorMessage(400, "text2", null);
        assertNotEquals(messageOne, messageThree);

        ErrorMessage messageFour = new ErrorMessage(400, null, null);
        assertNotEquals(messageOne, messageFour);
    }

    @Test
    public void TestErrorMessageToXml(){

        ErrorMessage msg  = new ErrorMessage(400, "hi", null);
        String expectedXml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<error>" +
                System.lineSeparator() +
                "  <code>400</code>" +
                System.lineSeparator() +
                "  <errorMessage>hi</errorMessage>" +
                System.lineSeparator() +
                "</error>" +
                System.lineSeparator();

        try {
            String xmlMsg = msg.toXml();
            assertEquals(expectedXml, xmlMsg);
        }
        catch(Exception ex){
            fail("can't convert from ErrorMessage to xml");
        }
    }

    @Test
    public void TestErrorMessagefromXml(){

        ErrorMessage expectedMsg  = new ErrorMessage(400, "hi", null);
        String inputXml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<error>" +
                System.lineSeparator() +
                "  <code>400</code>" +
                System.lineSeparator() +
                "  <errorMessage>hi</errorMessage>" +
                System.lineSeparator() +
                "</error>" +
                System.lineSeparator();

        try {
            ErrorMessage errorMessageFromXml = ErrorMessage.fromXml(inputXml);
            assertEquals(expectedMsg, errorMessageFromXml);
        }
        catch(Exception ex){
            fail("can't convert xml to ErrorMessage");
        }
    }

  @Test
  public void testConstructorAndGetters() {
    ErrorMessage errorMessage = new ErrorMessage(404, "Not Found", "ERR404");
    assertEquals(404, errorMessage.httpStatusCode);
    assertEquals("Not Found", errorMessage.message);
    assertEquals("ERR404", errorMessage.code);
  }

  @Test
  public void testEqualsAndHashCode() {
    ErrorMessage errorMessage1 = new ErrorMessage(404, "Not Found", "ERR404");
    ErrorMessage errorMessage2 = new ErrorMessage(404, "Not Found", "ERR404");
    ErrorMessage errorMessage3 = new ErrorMessage(500, "Internal Server Error", "ERR500");

    assertEquals(errorMessage1, errorMessage2);
    assertNotEquals(errorMessage1, errorMessage3);
    assertEquals(errorMessage1.hashCode(), errorMessage2.hashCode());
    assertNotEquals(errorMessage1.hashCode(), errorMessage3.hashCode());
  }

  @Test
  public void testToJson() throws JsonProcessingException {
    ErrorMessage errorMessage = new ErrorMessage(404, "Not Found", "ERR404");
    String json = errorMessage.toJson();
    assertNotNull(json);
    assertTrue(json.contains("\"code\" : 404"));
    assertTrue(json.contains("\"errorMessage\" : \"Not Found\""));
    assertTrue(json.contains("\"errorCode\" : \"ERR404\""));
  }

  @Test
  public void testToXml() throws JsonProcessingException {
    ErrorMessage errorMessage = new ErrorMessage(404, "Not Found", "ERR404");
    String xml = errorMessage.toXml();
    assertNotNull(xml);
    assertTrue(xml.contains("<code>404</code>"));
    assertTrue(xml.contains("<errorMessage>Not Found</errorMessage>"));
    assertTrue(xml.contains("<errorCode>ERR404</errorCode>"));
  }

  @Test
  public void testFromJson() throws IOException {
    String json = "{\"code\":404,\"errorMessage\":\"Not Found\",\"errorCode\":\"ERR404\"}";
    ErrorMessage errorMessage = ErrorMessage.fromJson(json);
    assertNotNull(errorMessage);
    assertEquals(404, errorMessage.httpStatusCode);
    assertEquals("Not Found", errorMessage.message);
    assertEquals("ERR404", errorMessage.code);
  }

  @Test
  public void testFromXml() throws IOException {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><code>404</code><errorMessage>Not Found</errorMessage><errorCode>ERR404</errorCode></error>";
    ErrorMessage errorMessage = ErrorMessage.fromXml(xml);
    assertNotNull(errorMessage);
    assertEquals(404, errorMessage.httpStatusCode);
    assertEquals("Not Found", errorMessage.message);
    assertEquals("ERR404", errorMessage.code);
  }

  @Test
  public void testBuilder() {
    ErrorMessage errorMessage = ErrorMessage.builder()
      .httpStatusCode(404)
      .errorMessage("Not Found")
      .errorCode("ERR404")
      .build();

    assertNotNull(errorMessage);
    assertEquals(404, errorMessage.httpStatusCode);
    assertEquals("Not Found", errorMessage.message);
    assertEquals("ERR404", errorMessage.code);
  }
}
