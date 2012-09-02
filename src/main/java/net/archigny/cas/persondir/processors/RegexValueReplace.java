package net.archigny.cas.persondir.processors;

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
public class RegexValueReplace implements IAttributesProcessor, InitializingBean {

    private Logger  log           = LoggerFactory.getLogger(RegexValueReplace.class);

    private String  key;

    private String  valueMatch;

    private String  valueReplace;

    private boolean caseSensitive = true;

    private Pattern valuePattern;

    @Override
    public void processAttributes(final Map<String, List<Object>> attributes) {

        final Set<String> attributeNames = attributes.keySet();
        for (final String attributeName : attributeNames) {
            log.debug("Considering attribute name : {}", attributeName);
            if (attributeName.equalsIgnoreCase(key)) {
                log.debug("Attribute found, applying value replacements");
                final List<Object> values = attributes.get(attributeName);
                int size = values.size();
                for (int i = 0; i < size; i++) {
                    final Object value = values.get(i);
                    if (value instanceof String) {
                        values.set(i, valuePattern.matcher(((String) value)).replaceAll(valueReplace));
                    }
                }
                break;
            } else {
                log.debug("Attribute does not match.");
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
        if (valueReplace == null) {
            throw new BeanCreationException("attrReplace cannot be null");
        }
        // Validate pattern.
        if (caseSensitive) {
            valuePattern = Pattern.compile(valueMatch);
        } else {
            valuePattern = Pattern.compile(valueMatch, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        log.debug("Configured to match attribute name : [{}] / values match [{}] replacement : [{}]", new Object[] { key,
                valueMatch, valueReplace });

    }

    // Setters and Getters

    public String getKey() {

        return key;
    }

    public void setKey(final String key) {

        this.key = key;
    }

    public String getValueMatch() {

        return valueMatch;
    }

    public void setValueMatch(final String valueMatch) {

        this.valueMatch = valueMatch;
    }

    public String getValueReplace() {

        return valueReplace;
    }

    public void setValueReplace(final String valueReplace) {

        this.valueReplace = valueReplace;
    }

    public boolean isCaseSensitive() {

        return caseSensitive;
    }

    public void setCaseSensitive(final boolean caseSensitive) {

        this.caseSensitive = caseSensitive;
    }

}
