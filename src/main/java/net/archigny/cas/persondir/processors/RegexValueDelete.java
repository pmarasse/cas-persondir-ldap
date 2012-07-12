package net.archigny.cas.persondir.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;

/**
 * Simple value regex processor. First search an attribute name with the value provided (case insensitive). If one found, it
 * processes all values with a replaceAll regex/replacement.<br />
 * Example : to get rid of LDAP clothing around memberOf attribute, one may set :
 * <ul>
 * <li>key = memberOf</li>
 * <li>valueMatch = ^cn=(.+),.+$</li>
 * <li>valueReplace = $1</li>
 * </ul>
 * 
 * @author philippe
 * 
 */
public class RegexValueDelete implements IAttributesProcessor, InitializingBean {

    private Logger  log           = LoggerFactory.getLogger(RegexValueDelete.class);

    private String  key;

    private String  valueMatch;

    private boolean caseSensitive = true;

    private Pattern valuePattern;

    @Override
    public void processAttributes(Map<String, List<Object>> attributes) {

        Set<String> attributeNames = attributes.keySet();
        for (String attributeName : attributeNames) {
            if (log.isDebugEnabled()) {
                log.debug("Considering attribute name : " + attributeName);
            }
            if (attributeName.equalsIgnoreCase(key)) {
                if (log.isDebugEnabled()) {
                    log.debug("Attribute found, searching values to delete");
                }
                List<Object> values = attributes.get(attributeName);
                List<Object> valuesToDelete = new ArrayList<Object>();

                int size = values.size();
                for (int i = 0; i < size; i++) {
                    Object value = values.get(i);
                    if (value instanceof String) {
                        if (valuePattern.matcher(((String) value)).matches()) {
                            valuesToDelete.add(value);
                        }
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug(valuesToDelete.size() + " values found to be deleted");
                }
                if (!valuesToDelete.isEmpty()) {
                    try {
                        values.removeAll(valuesToDelete);
                    } catch (UnsupportedOperationException e) {
                        for (Object valueToDelete : valuesToDelete) {
                            values.remove(valueToDelete);
                        }
                    }
                }
                break;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Attribute does not match.");
                }
            }
        }

    }

    /**
     * Since this implementation does not add attribute, return null
     */
    @Override
    public Set<String> getPossibleUserAttributeNames() {

        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        if (key == null) {
            throw new BeanCreationException("key cannot be null");
        }
        if (valueMatch == null) {
            throw new BeanCreationException("attrMatch cannot be null");
        }
        // Validate pattern.
        if (caseSensitive) {
            valuePattern = Pattern.compile(valueMatch);
        } else {
            valuePattern = Pattern.compile(valueMatch, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        if (log.isDebugEnabled()) {
            log.debug("Configured for attribute name : [" + key + "] / values match [" + valueMatch + "] ");
        }

    }

    // Setters and Getters

    public String getKey() {

        return key;
    }

    public void setKey(String key) {

        this.key = key;
    }

    public String getValueMatch() {

        return valueMatch;
    }

    public void setValueMatch(String valueMatch) {

        this.valueMatch = valueMatch;
    }

    public boolean isCaseSensitive() {

        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {

        this.caseSensitive = caseSensitive;
    }

}
