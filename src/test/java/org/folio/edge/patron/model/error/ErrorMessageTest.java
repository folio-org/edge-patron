package org.folio.edge.patron.model.error;
import org.junit.Test;
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
}
