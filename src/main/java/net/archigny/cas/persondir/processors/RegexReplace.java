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
 * Double regex processor :
 * <ul>
 * <li>A first regex is used to match attribute name</li>
 * <li>A second regex is used to do a replaceAll against all values</li>
 * </ul>
 * 
 * @author Philippe Marasse <philippe@archigny.net>
 * 
 */
public class RegexReplace implements IAttributesProcessor, InitializingBean {

    private Logger  log = LoggerFactory.getLogger(RegexReplace.class);

    private String  keyMatch;

    private String  valueMatch;

    private String  valueReplace;

    private Pattern keyPattern;

    private Pattern valuePattern;

    @Override
    public void processAttributes(final Map<String, List<Object>> attributes) {

        final Set<String> attributeNames = attributes.keySet();
        for (final String attributeName : attributeNames) {
            log.debug("Considering attribute name : ", attributeName);
            if (keyPattern.matcher(attributeName).find()) {
                log.debug("Attribute matches, applying value replacements");
                final List<Object> values = attributes.get(attributeName);
                int size = values.size();
                for (int i = 0; i < size; i++) {
                    final Object value = values.get(i);
                    if (value instanceof String) {
                        values.set(i, valuePattern.matcher(((String) value)).replaceAll(valueReplace));
                    }
                }
            } else {
                log.debug("Attribute does not match.");
            }
        }
    }

    /**
     * Since there is no attribute added by this implementation, return null
     */
    @Override
    public Set<String> getPossibleUserAttributeNames() {

        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        if (keyMatch == null) {
            throw new BeanCreationException("keyMatch cannot be null");
        }
        if (valueMatch == null) {
            throw new BeanCreationException("attrMatch cannot be null");
        }
        if (valueReplace == null) {
            throw new BeanCreationException("attrReplace cannot be null");
        }
        // Validate patterns.
        keyPattern = Pattern.compile(keyMatch);
        valuePattern = Pattern.compile(valueMatch);
        log.debug("Configured to match attribute names : [{}] / values match [{}] replacement : [{}]", new Object[] { keyMatch,
                valueMatch, valueReplace });
    }

    // Getters and setters

    public String getKeyMatch() {

        return keyMatch;
    }

    public void setKeyMatch(final String keyMatch) {

        this.keyMatch = keyMatch;
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

}
