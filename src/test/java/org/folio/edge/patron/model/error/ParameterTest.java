package org.folio.edge.patron.model.error;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ParameterTest {

    @Test
    public void set_get_AdditionalProperties(){

        Parameter param = new Parameter();

        param.setAdditionalProperty("prop1", "property1");
        param.setAdditionalProperty("prop2", "property2");
        param.setAdditionalProperty("prop3", "property3");

        Map<String, Object> addlProps = param.getAdditionalProperties();

        assertEquals(3, addlProps.size());
        assertEquals( "property1", addlProps.get("prop1"));
        assertEquals( "property2", addlProps.get("prop2"));
        assertEquals( "property3", addlProps.get("prop3"));


        param.withAdditionalProperty("prop4", "property4");
        assertEquals("property4", param.getAdditionalProperties().get("prop4"));
    }

    @Test
    public void valueTests()
    {
        Parameter param = new Parameter();
        param.setValue("value1");

        String paramValue = param.getValue();
        assertEquals("value1", paramValue);

        Parameter returnedParam = param.withValue("value2");
        assertNotNull(returnedParam);
        assertEquals(param, returnedParam);
        assertEquals("value2", returnedParam.getValue());
    }

    @Test
    public void keyTests()
    {
        Parameter param = new Parameter();
        param.setKey("key1");

        String paramKey = param.getKey();
        assertEquals("key1", paramKey);

        Parameter returnedParam = param.withKey("key2");
        assertNotNull(returnedParam);
        assertEquals(param, returnedParam);
        assertEquals("key2", returnedParam.getKey());
    }
}
