/**
 * 
 */
package net.archigny.cas.persondir.processors;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.archigny.cas.persondir.processors.RegexValueReplace;

import org.junit.Test;


/**
 * @author philippe
 *
 */
public class RegexReplaceTest {

    
    /**
     * Test method for {@link net.archigny.persondir.ldap.processors.RegexValueReplace#processAttributes(java.util.Map)}.
     * @throws Exception 
     */
    @Test
    public void testProcessAttributesCaseSensitive() throws Exception {

        Map<String, List<Object>> attrs = PersonAttributesSetup.getPersonAttributes(); 

        RegexValueReplace test = new RegexValueReplace();
        test.setKey("memberof");
        test.setValueMatch("cn=(.+),\\s*ou=.*");
        test.setValueReplace("$1");
        test.setCaseSensitive(true);
        test.afterPropertiesSet();
        
        test.processAttributes(attrs);
        
        List<Object> memberOf = attrs.get("memberOf");
        
        List<Object> expected = new ArrayList<Object>();
        expected.add("Groupe 1");
        expected.add("Groupe, deux");
        expected.add("Groupe à accents");
        expected.add("Groupe 4");
        expected.add("Groupe 5");
        
        assertEquals(expected, memberOf);
    }

    /**
     * Test method for {@link net.archigny.persondir.ldap.processors.RegexValueReplace#processAttributes(java.util.Map)}.
     * @throws Exception 
     */
    @Test
    public void testProcessAttributesCaseInsensitive() throws Exception {

        Map<String, List<Object>> attrs = PersonAttributesSetup.getPersonAttributes(); 

        RegexValueReplace test = new RegexValueReplace();
        test.setKey("memberof");
        test.setValueMatch("CN=(.+),\\s*OU=.*");
        test.setValueReplace("$1");
        test.setCaseSensitive(false);
        test.afterPropertiesSet();
        
        test.processAttributes(attrs);
        
        List<Object> memberOf = attrs.get("memberOf");
        
        List<Object> expected = new ArrayList<Object>();
        expected.add("Groupe 1");
        expected.add("Groupe, deux");
        expected.add("Groupe à accents");
        expected.add("Groupe 4");
        expected.add("Groupe 5");
        
        assertEquals(expected, memberOf);
    }
}
