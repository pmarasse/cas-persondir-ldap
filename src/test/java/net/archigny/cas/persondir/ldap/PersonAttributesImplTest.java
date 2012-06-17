package net.archigny.cas.persondir.ldap;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.archigny.cas.persondir.processors.PersonAttributesSetup;

import org.junit.Before;
import org.junit.Test;

public class PersonAttributesImplTest {

    private PersonAttributesImpl person;

    @Before
    public void setUp() throws Exception {

        person = new PersonAttributesImpl("pmarasse", PersonAttributesSetup.getPersonAttributes());
    }

    @Test
    public void testLock() {

        assertFalse(person.isLocked());
        person.lock();
        assertTrue(person.isLocked());
        Map<String, List<Object>> attrs = person.getAttributes();

        try {
            attrs.put("test", new ArrayList<Object>());
            fail("Adding an attribute should throw an exception");
        } catch (Exception e) {
            // Exception attendue
        }

        List<Object> snList = attrs.get("sn");
        try {
            snList.add("testSN");
            fail("Adding a value to an attribute should throw an exception");
        } catch (Exception e) {
            // Exception attendue
        }

    }

    @Test
    public void testGetAttributeValue() {

        String sn = (String) person.getAttributeValue("sn");
        String givenName = (String) person.getAttributeValue("givenName");
        String mail = (String) person.getAttributeValue("mail");

        assertEquals("Marasse", sn);
        assertEquals("Philippe", givenName);
        assertEquals("pmarasse@test.archigny.net", mail);
    }

    @Test
    public void testGetAttributeValues() {

        List<Object> groupes = person.getAttributeValues("memberOf");
        assertTrue(groupes.contains("cn=Groupe 1, ou=Groupes, dc=archigny, dc=net"));
        assertTrue(groupes.contains("cn=Groupe, deux, ou=Groupes, dc=archigny, dc=net"));
        assertTrue(groupes.contains("cn=Groupe à accents, ou=Groupes, dc=archigny, dc=net"));
        assertTrue(groupes.contains("cn=Groupe 4, ou=Groupes, dc=archigny, dc=net"));
        assertTrue(groupes.contains("cn=Groupe 5, ou=Groupes, dc=archigny, dc=net"));
        assertEquals(5, groupes.size());
    }

    @Test
    public void testGetName() {
        assertEquals("pmarasse",person.getName());
    }
    
}
