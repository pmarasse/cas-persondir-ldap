/**
 * 
 */
package net.archigny.cas.persondir.processors;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.archigny.cas.persondir.processors.RegexReplace;
import org.junit.Test;


/**
 * @author philippe
 *
 */
public class RegexValueReplaceTest {

    
    /**
     * Test method for {@link net.archigny.persondir.processors.RegexValueReplace#processAttributes(java.util.Map)}.
     * @throws Exception 
     */
    @Test
    public void testProcessAttributes() throws Exception {

        Map<String, List<Object>> attrs = PersonAttributesSetup.getPersonAttributes(); 

        RegexReplace test = new RegexReplace();
        test.setKeyMatch("^.n");
        test.setValueMatch("^(.+)sse");
        test.setValueReplace("$1t");
        test.afterPropertiesSet();
        
        test.processAttributes(attrs);
        
        List<Object> cn = attrs.get("cn");
        List<Object> sn = attrs.get("sn");
        List<Object> ssn = attrs.get("ssn");
        
        // Matched name and value
        List<Object> expectedCn = new ArrayList<Object>();
        expectedCn.add("Philippe Marat");
        
        // Matched name and value
        List<Object> expectedSn = new ArrayList<Object>();
        expectedSn.add("Marat");        

        // Matched value, but not matched name
        List<Object> expectedSsn = new ArrayList<Object>();
        expectedSsn.add("No Match Marasse");        
        
        assertEquals(expectedCn, cn);
        assertEquals(expectedSn, sn);
        assertEquals(expectedSsn, ssn);
    }

}
