package net.archigny.cas.persondir.ldap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersonAttributesImpl implements ILockablePersonAttributes {

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

    // Implementation of ILockablePersonAttributes

    /**
     * Transforms attributes collection to an unmodfiable collection
     */
    @Override
    public void lock() {

        if (!locked) {
            // Lock Every collection of attribute values
            Set<String> attrNames = attributes.keySet();
            for (String attrName : attrNames) {
                List<Object> attrValues = attributes.get(attrName);
                attributes.put(attrName,Collections.unmodifiableList(attrValues));
            }
            // Finally lock the main collection
            attributes = Collections.unmodifiableMap(attributes);
            locked = true;
        }

    }

    @Override
    public boolean isLocked() {
    
        return locked;
    }

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

    @Override
    public String toString() {

        return "PersonAttributes [name=" + name + ", attributes=" + attributes + "]";
    }

}
