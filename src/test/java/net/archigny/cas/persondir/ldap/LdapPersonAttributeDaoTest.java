package net.archigny.cas.persondir.ldap;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import net.archigny.cas.persondir.processors.AddTodayDateProcessor;
import net.archigny.cas.persondir.processors.IAttributesProcessor;

import org.jasig.services.persondir.IPersonAttributes;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.ldap.core.ContextSource;

public class LdapPersonAttributeDaoTest {

    private Logger             log     = LoggerFactory.getLogger(LdapPersonAttributeDaoTest.class);

    private ContextSource  ldapCS;

    public final static String BASE_DN = "ou=comptes,dc=test,dc=archigny,dc=net";

    public final static String DN_ATTR = "userDn";

    @Before
    public void setUp() throws Exception {

        log.info("Initialise l'application de test à partir du XML spring");
    	ApplicationContext testApp = new ClassPathXmlApplicationContext("app-LdapPersonAttributeDaoTest.xml");
        ldapCS = (ContextSource) testApp.getBean("searchContextSource");
    	
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

    @Test
    public void getPersonWithEmail() {

        log.info("Test : getPersonWithEmail() - test ldapfilter + mapping");

        LdapPersonAttributeDao ldapDAO = new LdapPersonAttributeDao();
        ldapDAO.setContextSource(ldapCS);
        ldapDAO.setBaseDN(BASE_DN);

        ArrayList<String> rawAttrs = new ArrayList<String>(3);
        rawAttrs.add("cn");
        rawAttrs.add("sn");
        rawAttrs.add("uid");
        rawAttrs.add("memberOf");
        ldapDAO.setQueriedAttributes(rawAttrs);

        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("sn", "nom");
        mapping.put("cn", "nomCommun");
        mapping.put("givenname", "prenom");
        ldapDAO.setResultAttributeMapping(mapping);

        ldapDAO.setLdapFilter("mail={0}");

        try {
            ldapDAO.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception levée au AfterPropertiesSet");
        }

        IPersonAttributes personAttributes = ldapDAO.getPerson("scroche@test.archigny.net");
        System.out.println(personAttributes);
        assertNotNull(personAttributes);
        assertEquals("Sarah", personAttributes.getAttributeValue("prenom"));
        assertEquals("Croche", personAttributes.getAttributeValue("nom"));
        assertEquals("scroche", personAttributes.getAttributeValue("uid"));
    }

    @Test
    public void testTwoProcessors() {

        log.info("Test : oneProcessor() - test mapping + 1 processor");

        LdapPersonAttributeDao ldapDAO = new LdapPersonAttributeDao();
        ldapDAO.setContextSource(ldapCS);
        ldapDAO.setBaseDN(BASE_DN);

        ArrayList<String> rawAttrs = new ArrayList<String>();
        rawAttrs.add("cn");
        rawAttrs.add("sn");
        rawAttrs.add("uid");
        ldapDAO.setQueriedAttributes(rawAttrs);

        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("sn", "nom");
        mapping.put("cn", "nomCommun");
        mapping.put("givenName", "prenom");
        mapping.put("memberOf", "groupe");
        ldapDAO.setResultAttributeMapping(mapping);

        ArrayList<IAttributesProcessor> processors = new ArrayList<IAttributesProcessor>(1);
        processors.add(new AddTodayDateProcessor());

        ldapDAO.setProcessors(processors);

        try {
            ldapDAO.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception levée au AfterPropertiesSet");
        }

        // Bugfix : returning null by getPossibleUserAttributeNames throwed nullPointerException
        processors.add(new IAttributesProcessor() {

            @Override
            public void processAttributes(Map<String, List<Object>> attributes) {

            }

            @Override
            public Set<String> getPossibleUserAttributeNames() {

                return null;
            }
        });

        Set<String> attrs = ldapDAO.getPossibleUserAttributeNames();

        System.out.println(attrs);
        assertTrue(attrs.contains(AddTodayDateProcessor.DATE_ATTRIBUTE_NAME));
        assertTrue(attrs.contains("uid"));
        assertTrue(attrs.contains("nom"));
        assertTrue(attrs.contains("prenom"));
        assertTrue(attrs.contains("groupe"));
        assertTrue(attrs.contains("nomCommun"));
        assertEquals(6, attrs.size());

        IPersonAttributes personAttributes = ldapDAO.getPerson("ghouse");
        System.out.println(personAttributes);
        assertNotNull(personAttributes);
        assertNotNull(personAttributes.getAttributeValue(AddTodayDateProcessor.DATE_ATTRIBUTE_NAME));
        assertEquals("Gregory", personAttributes.getAttributeValue("prenom"));
        assertEquals("House", personAttributes.getAttributeValue("nom"));
        assertEquals("ghouse", personAttributes.getAttributeValue("uid"));
    }

    @Test
    public void DnAttributeTest() {

        log.info("DnAttributeTest : adding user DN in attributes");
        
        LdapPersonAttributeDao ldapDAO = new LdapPersonAttributeDao();
        ldapDAO.setContextSource(ldapCS);
        ldapDAO.setBaseDN(BASE_DN);

        ArrayList<String> rawAttrs = new ArrayList<String>();
        rawAttrs.add("cn");
        rawAttrs.add("sn");
        rawAttrs.add("uid");
        ldapDAO.setQueriedAttributes(rawAttrs);

        ldapDAO.setDnAttributeName(DN_ATTR);

        try {
            ldapDAO.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception levée au AfterPropertiesSet");
        }

        Set<String> attrs = ldapDAO.getPossibleUserAttributeNames();
        assertEquals(4, attrs.size());
        assertTrue(attrs.contains("uid"));
        assertTrue(attrs.contains("cn"));
        assertTrue(attrs.contains("sn"));
        assertTrue(attrs.contains(DN_ATTR));
        
        IPersonAttributes personAttributes = ldapDAO.getPerson("ghouse");
        System.out.println(personAttributes);
        assertNotNull(personAttributes);
        
        assertEquals("House", personAttributes.getAttributeValue("sn"));
        assertEquals("ghouse", personAttributes.getAttributeValue("uid"));

        try {
            LdapName expectedDN = new LdapName("cn=Gregory House,ou=Comptes,dc=test,dc=archigny,dc=net");
            LdapName actualDN = new LdapName((String) personAttributes.getAttributeValue(DN_ATTR));
            assertEquals(expectedDN, actualDN);
        } catch (InvalidNameException e) {
            e.printStackTrace();
            fail("Exception levée à la création d'un des LdapName ???");
        }
        
        
    }

}
