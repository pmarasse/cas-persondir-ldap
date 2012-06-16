package net.archigny.cas.persondir.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PersonAttributesSetup {

    public static Map<String,List<Object>> getPersonAttributes() {
        Map<String,List<Object>> attributes = new HashMap<String, List<Object>>();
        List<Object> values = new ArrayList<Object>();
        values.add("Philippe");
        attributes.put("givenName", values);

        values = new ArrayList<Object>();
        values.add("Marasse");
        attributes.put("sn", values);
        
        values = new ArrayList<Object>();
        values.add("Philippe Marasse");
        attributes.put("cn", values);
        
        values = new ArrayList<Object>();
        values.add("pmarasse@test.archigny.net");
        attributes.put("mail", values);
        
        values = new ArrayList<Object>();
        values.add("No Match Marasse");
        attributes.put("ssn", values);        
        
        values = new ArrayList<Object>();
        values.add("cn=Groupe 1, ou=Groupes, dc=archigny, dc=net");
        values.add("cn=Groupe, deux, ou=Groupes, dc=archigny, dc=net");
        values.add("cn=Groupe à accents, ou=Groupes, dc=archigny, dc=net");
        values.add("cn=Groupe 4, ou=Groupes, dc=archigny, dc=net");
        values.add("cn=Groupe 5, ou=Groupes, dc=archigny, dc=net");
        attributes.put("memberOf", values);
        
        return attributes;
    }
    
}
