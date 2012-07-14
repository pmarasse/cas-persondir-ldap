package net.archigny.cas.persondir.processors;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.archigny.cas.persondir.ldap.ILockablePersonAttributes;
import net.archigny.cas.persondir.ldap.PersonAttributesImpl;

import org.junit.Before;
import org.junit.Test;

public class AttributeValueToAttributeTest {

	private ILockablePersonAttributes personAttributes;
	
	private static String GROUPE_ATTR = "groupes";
	
	private static String USERNAME = "testuser";
	
	private static String GROUP1 = "Groupe à moi";
	
	private static String GROUP2 = "web-Rédacteur Pôle A";

	private static String GROUP3 = "web-Rédacteur Pôle B";

	private static String GROUP4 = "Groupe test";
	
	private static String PREFIX = "web-";

	@Before
	public void setUpBefore() throws Exception {
		Map<String, List<Object>> attrs = new HashMap<String, List<Object>>();
		personAttributes = new PersonAttributesImpl(USERNAME, attrs);
		
		List<Object> prenom = new ArrayList<Object>();
		prenom.add("Test");
		attrs.put("prenom", prenom);
		
		List<Object> nom = new ArrayList<Object>();
		nom.add("PM");
		attrs.put("nom", nom);
		
		List<Object> groupes = new ArrayList<Object>();
		groupes.add(GROUP1);
		groupes.add(GROUP2);
		groupes.add(GROUP3);
		groupes.add(GROUP4);
		attrs.put(GROUPE_ATTR, groupes);
	}

	@Test
	public void testPossibleAttributes() {
		
		AttributeValueToAttribute processor = new AttributeValueToAttribute();
		try {
			processor.afterPropertiesSet();
			fail("Doit foirer sans paramètres");
		} catch (Exception e) {
		}
		processor.setAttributeName(GROUPE_ATTR);
		try {
			processor.afterPropertiesSet();
			fail("Doit foirer avec un seul paramètre");
		} catch (Exception e) {
		}
		
		Map<String,String> prefixToAttrs = new HashMap<String, String>();
		prefixToAttrs.put(PREFIX,"drupal");
		prefixToAttrs.put("webzz-","drupal2");
		
		processor.setPrefixToAttributeName(prefixToAttrs);
		try {
			processor.afterPropertiesSet();
		} catch (Exception e) {
			fail("Doit passer le afterPropertiesSet");
		}
		
		System.out.println(personAttributes);
		processor.processAttributes(personAttributes.getAttributes());
		System.out.println(personAttributes);
		List<Object> drupal = personAttributes.getAttributeValues("drupal");
		assertNotNull(drupal);
		assertEquals(2, drupal.size());
		assertNull(personAttributes.getAttributeValues("drupal2"));
		
		String val1 = GROUP2.substring(PREFIX.length());
		String val2 = GROUP3.substring(PREFIX.length());
		assertTrue(drupal.contains(val1));
		assertTrue(drupal.contains(val2));
	}
	
	@Test
	public void possibleAttributesTest() {
		AttributeValueToAttribute processor = new AttributeValueToAttribute();
		processor.setAttributeName(GROUPE_ATTR);
		Map<String,String> prefixToAttrs = new HashMap<String, String>();
		prefixToAttrs.put(PREFIX,"drupal");
		prefixToAttrs.put("webzz-","drupal2");
		processor.setPrefixToAttributeName(prefixToAttrs);
		
		try {
			processor.afterPropertiesSet();
		} catch (Exception e) {
			fail("Doit passer le afterPropertiesSet");
		}
		
		Set<String> possibleAttrs = processor.getPossibleUserAttributeNames();
		assertEquals(2, possibleAttrs.size());
		assertTrue(possibleAttrs.contains("drupal"));
		assertTrue(possibleAttrs.contains("drupal2"));
	}
	
}
