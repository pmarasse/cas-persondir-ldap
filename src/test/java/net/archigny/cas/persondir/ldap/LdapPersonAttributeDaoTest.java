package net.archigny.cas.persondir.ldap;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.jasig.services.persondir.IPersonAttributes;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.support.LdapContextSource;

public class LdapPersonAttributeDaoTest {

    private Logger             log     = LoggerFactory.getLogger(LdapPersonAttributeDaoTest.class);

    private LdapContextSource  ldapCS  = new LdapContextSource();

    public final static String BASE_DN = "ou=comptes,dc=test,dc=archigny,dc=net";

    @Before
    public void setUp() throws Exception {

        log.info("Initialise la ContextSource");
        ldapCS.setUrl("ldap://orthanc.archigny.net/");
        ldapCS.setUserDn("cn=proxy,dc=test,dc=archigny,dc=net");
        ldapCS.setPassword("proxyTest");
        ldapCS.afterPropertiesSet();
        log.info("ldapCS Initialisée");
    }

    @Test
    public void baseTest() {

        log.info("Test : baseTest()");
        LdapPersonAttributeDao ldapDAO = new LdapPersonAttributeDao();
        
        try {
            ldapDAO.setBaseDN(null);
            fail("Exception non levée setBaseDN null");
        } catch (Exception e) {
        }

        try {
            ldapDAO.setContextSource(null);
            fail("Exception non levée setContextSource null");
        } catch (Exception e) {
        }
        try {
            ldapDAO.setLdapFilter(null);
            fail("Exception non levée setLdapFilter null");
        } catch (Exception e) {
        }
        try {
            ldapDAO.setProcessors(null);
            fail("Exception non levée setProcessors null");
        } catch (Exception e) {
        }
        try {
            ldapDAO.setQueriedAttributes(null);
            fail("Exception non levée setQueriedAttributes null");
        } catch (Exception e) {
        }
        try {
            ldapDAO.setResultAttributeMapping(null);
            fail("Exception non levée setResultAttributeMapping null");
        } catch (Exception e) {
        }
        
        
        try {
            ldapDAO.afterPropertiesSet();
            fail("Exception non levée au AfterPropertiesSet alors que le contextSource n'est pas fourni");
        } catch (Exception e) {
        }
        
        ldapDAO.setContextSource(ldapCS);
        try {
            ldapDAO.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception levée au AfterPropertiesSet avec ContextSource fixée ??");
        }
        
        IPersonAttributes result = ldapDAO.getPerson("inexistent-uid");
        assertNull(result);
    }

    @Test
    public void simpleTest() {

        log.info("Test : simpleTest()");

        LdapPersonAttributeDao ldapDAO = new LdapPersonAttributeDao();
        ldapDAO.setContextSource(ldapCS);
        ldapDAO.setBaseDN(BASE_DN);

        ArrayList<String> rawAttrs = new ArrayList<String>(3);
        rawAttrs.add("cn");
        rawAttrs.add("sn");
        rawAttrs.add("givenName");
        rawAttrs.add("memberOf");
        ldapDAO.setQueriedAttributes(rawAttrs);

        try {
            ldapDAO.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception levée au AfterPropertiesSet");
        }
        IPersonAttributes personAttributes = ldapDAO.getPerson("ghouse");
        System.out.println(personAttributes);
        assertNotNull(personAttributes);
        assertEquals("Gregory", personAttributes.getAttributeValue("givenName"));
        assertEquals("House", personAttributes.getAttributeValue("sn"));
    }

    @Test
    public void getPossibleAttributeNamesTest() {

        log.info("Test : getPossibleAttributeNamesTest()");

        LdapPersonAttributeDao ldapDAO = new LdapPersonAttributeDao();
        ldapDAO.setContextSource(ldapCS);
        ldapDAO.setBaseDN(BASE_DN);

        ArrayList<String> rawAttrs = new ArrayList<String>(3);
        rawAttrs.add("cn");
        rawAttrs.add("sn");
        rawAttrs.add("givenName");
        rawAttrs.add("memberOf");
        ldapDAO.setQueriedAttributes(rawAttrs);

        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("sn", "nom");
        mapping.put("cn", "nomCommun");
        mapping.put("givenName", "prenom");
        ldapDAO.setResultAttributeMapping(mapping);

        try {
            ldapDAO.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception levée au AfterPropertiesSet");
        }
        Set<String> attrs = ldapDAO.getPossibleUserAttributeNames();

        System.out.println(attrs);
        assertTrue(attrs.contains("nom"));
        assertTrue(attrs.contains("prenom"));
        assertTrue(attrs.contains("memberOf"));
        assertTrue(attrs.contains("nomCommun"));
        assertEquals(4, attrs.size());
    }

    @Test
    public void getPersonWithMapping() {

        log.info("Test : getPersonWithMapping()");

        LdapPersonAttributeDao ldapDAO = new LdapPersonAttributeDao();
        ldapDAO.setContextSource(ldapCS);
        ldapDAO.setBaseDN(BASE_DN);

        ArrayList<String> rawAttrs = new ArrayList<String>(3);
        rawAttrs.add("cn");
        rawAttrs.add("sn");
        rawAttrs.add("uid");
        rawAttrs.add("givenName");
        rawAttrs.add("memberOf");
        ldapDAO.setQueriedAttributes(rawAttrs);

        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("sn", "nom");
        mapping.put("cn", "nomCommun");
        mapping.put("givenName", "prenom");
        ldapDAO.setResultAttributeMapping(mapping);

        try {
            ldapDAO.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception levée au AfterPropertiesSet");
        }
        IPersonAttributes personAttributes = ldapDAO.getPerson("lcuddy");
        System.out.println(personAttributes);
        assertNotNull(personAttributes);
        assertEquals("Lisa", personAttributes.getAttributeValue("prenom"));
        assertEquals("Cuddy", personAttributes.getAttributeValue("nom"));
        assertEquals("lcuddy", personAttributes.getAttributeValue("uid"));

    }

    // TODO : test ldapFilters, processors
    
}
