package org.folio.edge.patron.model.error;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ErrorTest {

    @Test
    public void typeTests(){
        Error error = new Error();


        error.withType("typeA");
        String aCode = error.getType();
        assertEquals("typeA", aCode);

        assertEquals("typeA", error.getType());

        error.setType("typeB");
        assertEquals("typeB", error.getType());
    }

    @Test
    public void codeTests(){
        Error error = new Error();


        error.withCode("CodeRed");
        String aCode = error.getCode();
        assertEquals("CodeRed", aCode);

        assertEquals("CodeRed", error.getCode());

        error.setCode("CodeBlue");
        assertEquals("CodeBlue", error.getCode());
    }

    @Test
    public void additionalPropertiesTests(){

        Error error = new Error();

        error.setAdditionalProperty("prop1", "property1");
        error.setAdditionalProperty("prop2", "property2");
        error.setAdditionalProperty("prop3", "property3");

        Map<String, Object> addlProps = error.getAdditionalProperties();

        assertEquals(3, addlProps.size());
        assertEquals( "property1", addlProps.get("prop1"));
        assertEquals( "property2", addlProps.get("prop2"));
        assertEquals( "property3", addlProps.get("prop3"));


        error.withAdditionalProperty("prop4", "property4");
        assertEquals("property4", error.getAdditionalProperties().get("prop4"));
    }

    @Test
    public void messageTests(){
        Error error = new Error();

        error.withMessage("small message");
        String theMessage = error.getMessage();

        assertEquals("small message", theMessage);

        error.setMessage("big message");
        assertEquals("big message", error.getMessage());
    }

    @Test
    public void parameterTests(){
        Error error = new Error();

        Parameter param1 = new Parameter();
        param1.setKey("key1");
        param1.setValue("val1");

        Parameter param2 = new Parameter();
        param2.setKey("key2");
        param2.setValue("val2");

        List<Parameter> parameterList = new ArrayList<>();
        parameterList.add(param1);
        parameterList.add(param2);

        error.withParameters(parameterList);
        List<Parameter> returnedParams = error.getParameters();

        assertNotNull(returnedParams);
        assertEquals(parameterList, returnedParams);
        assertEquals(parameterList.size(), returnedParams.size());

        List<Parameter> list3 = new ArrayList<>();
        list3.add(param1);
        error.setParameters(list3);

        List<Parameter> returnedList3 = error.getParameters();

        assertNotNull(returnedList3);
        assertEquals(list3, returnedList3);
        assertEquals(list3.size(), returnedList3.size());
    }
}
