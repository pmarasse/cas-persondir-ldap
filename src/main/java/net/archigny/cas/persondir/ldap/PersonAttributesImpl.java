package net.archigny.cas.persondir.ldap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jasig.services.persondir.IPersonAttributes;

public class PersonAttributesImpl implements IPersonAttributes {

    private static final long         serialVersionUID = 1821405503021254743L;

    private String                    name;

    private Map<String, List<Object>> attributes       = new HashMap<String, List<Object>>();

    private boolean                   locked           = false;

    public PersonAttributesImpl() {

    }

    /**
     * @param name
     *            Nom de l'utilisateur (uid)
     * @param attributes
     *            HashMap des attributs;
     */
    public PersonAttributesImpl(String name, Map<String, List<Object>> attributes) {

        this.name = name;
        this.attributes = attributes;
    }

    /**
     * Transforms attributes collection to an unmodfiable collection
     */
    public void lock() {

        if (!locked) {
            attributes = Collections.unmodifiableMap(attributes);
            locked = true;
        }

    }

    // Implementation of IPersonAttributes

    @Override
    public String getName() {

        return name;
    }

    @Override
    public Map<String, List<Object>> getAttributes() {

        return attributes;
    }

    @Override
    public Object getAttributeValue(String name) {

        List<Object> values;
        if ((values = attributes.get(name)) != null) {
            if (values.size() > 0) {
                return values.get(0);
            }
        }
        return null;
    }

    @Override
    public List<Object> getAttributeValues(String name) {

        return attributes.get(name);
    }

    // Getters and Setters

    public void setName(String name) {

        this.name = name;
    }

    public void setAttributes(Map<String, List<Object>> attributes) {

        this.attributes = attributes;
    }

    
    public boolean isLocked() {
    
        return locked;
    }

    @Override
    public String toString() {

        return "PersonAttributes [name=" + name + ", attributes=" + attributes + "]";
    }

}
