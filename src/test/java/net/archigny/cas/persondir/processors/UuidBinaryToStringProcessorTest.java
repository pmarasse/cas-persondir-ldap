package net.archigny.cas.persondir.processors;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UuidBinaryToStringProcessorTest {

    public final Logger                 log         = LoggerFactory.getLogger(UuidBinaryToStringProcessorTest.class);

    public final static byte[]          TOKEN_1     = Base64.decodeBase64("oIquu0CtoEaWynIU3CjfKA==");

    public final static String          UUID_1_NAME = "A08AAEBB-40AD-A046-96CA-7214DC28DF28";

    public final static byte[]          TOKEN_2     = Base64.decodeBase64("AuqPDY2V1EGcoTrn4pHLJg==");

    public final static String          UUID_2_NAME = "02EA8F0D-8D95-D441-9CA1-3AE7E291CB26";

    public final static String          SOURCE_ATTR = "sourceUuid";

    public final static String          TARGET_ATTR = "uuid";

    private Map<String, List<Object>>   attributes  = new HashMap<String, List<Object>>();

    private UuidBinaryToStringProcessor processor   = new UuidBinaryToStringProcessor();

    @Before
    public void setUp() throws Exception {

        log.info("setting up attributes repository");
        attributes = PersonAttributesSetup.getPersonAttributes();

        List<Object> values = new ArrayList<Object>();
        values.add(TOKEN_1);
        values.add(TOKEN_2);

        attributes.put(SOURCE_ATTR, values);
    }

    @Test
    public void simpleTest() throws Exception {

        log.info("simpleTest begins.");

        try {
            processor.afterPropertiesSet();
            fail("processor with no arguments must fail");
        } catch (Exception e) {
            // expected behaviour
        }

        processor.setSourceAttribute(SOURCE_ATTR);

        try {
            processor.afterPropertiesSet();
            fail("processor with only source attribute must fail");
        } catch (Exception e) {
            // expected behaviour
        }

        processor.setTargetAttribute(TARGET_ATTR);

        processor.afterPropertiesSet();

        // Verify that target attribute name is returned
        assertEquals(TARGET_ATTR, processor.getPossibleUserAttributeNames().iterator().next());

        processor.processAttributes(attributes);

        List<Object> values = attributes.get(TARGET_ATTR);

        assertNotNull(values);

        log.info("decoded value : " + values.get(0));
        assertEquals(UUID_1_NAME, values.get(0));

        log.info("decoded value : " + values.get(1));
        assertEquals(UUID_2_NAME, values.get(1));

    }

    @Test
    public void illegalArgumentsTest() throws Exception {

        log.info("illegalArgumentsTest - no exception");

        List<Object> values = attributes.get(SOURCE_ATTR);

        values.add("Error String !");

        int size = values.size();

        processor.setSourceAttribute(SOURCE_ATTR);
        processor.setTargetAttribute(TARGET_ATTR);
        processor.setDeleteSourceAttribute(false);
        processor.afterPropertiesSet();

        processor.processAttributes(attributes);
        log.debug("Returned attributes : " + Arrays.toString(attributes.get(TARGET_ATTR).toArray()));
        assertEquals(attributes.get(TARGET_ATTR).size(), size);

        log.info("set exception ON");
        processor.setRaiseIllegalFormatException(true);
        attributes.remove(TARGET_ATTR);
        try {
            processor.processAttributes(attributes);
            fail("Should raise an IllegalArgumentException !");
        } catch (IllegalArgumentException e) {

        }

        long l = 12453;
        values.remove(2);
        values.add(l);
        try {
            processor.processAttributes(attributes);
            fail("Should raise an IllegalArgumentException !");
        } catch (IllegalArgumentException e) {

        }
        
        
    }

    @Test
    public void noProcessingTest() throws Exception {
        log.info("no Processing Test");
        attributes.remove(SOURCE_ATTR);
        processor.setSourceAttribute(SOURCE_ATTR);
        processor.setTargetAttribute(TARGET_ATTR);
        processor.afterPropertiesSet();

        processor.processAttributes(attributes);
        
        assertFalse(attributes.containsKey(TARGET_ATTR));
        
    }
    
}
