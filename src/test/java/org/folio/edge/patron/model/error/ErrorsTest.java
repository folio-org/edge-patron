package org.folio.edge.patron.model.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ErrorsTest {

    @Test
    void withErrorsTest(){

        Error error1 = new Error();
        Error error2 = new Error();
        Error error3 = new Error();

        List<Error> errorList = new ArrayList<>();
        errorList.add(error1);
        errorList.add(error2);
        errorList.add(error3);

        Errors errors = new Errors();
        Errors errors2 = errors.withErrors(errorList);

        assertEquals(errors, errors2);
        assertEquals(3, errors.getErrors().size());
        assertEquals(error1, errors.getErrors().get(0));
        assertEquals(error2, errors.getErrors().get(1));
        assertEquals(error3, errors.getErrors().get(2));
    }

    @Test
    void totalRecordsTests(){
        Errors errors = new Errors();
        Errors returnedErrors = errors.withTotalRecords(5);


        assertEquals(errors, returnedErrors);
        assertEquals(5, returnedErrors.getTotalRecords().intValue());


        errors.setTotalRecords(7);
        int totalRecords = errors.getTotalRecords();
        assertEquals(7, totalRecords);
    }

    @Test
    void additionalPropertiesTests(){

        Errors errors = new Errors();

        errors.setAdditionalProperty("prop1", "property1");
        errors.setAdditionalProperty("prop2", "property2");
        errors.setAdditionalProperty("prop3", "property3");

        Map<String, Object> addlProps = errors.getAdditionalProperties();

        assertEquals(3, addlProps.size());
        assertEquals( "property1", addlProps.get("prop1"));
        assertEquals( "property2", addlProps.get("prop2"));
        assertEquals( "property3", addlProps.get("prop3"));


        errors.withAdditionalProperty("prop4", "property4");
        assertEquals("property4", errors.getAdditionalProperties().get("prop4"));
    }

}
