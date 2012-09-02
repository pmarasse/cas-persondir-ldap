package net.archigny.cas.persondir.processors;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sample processor class. Just add a "date" attribute with the current date.
 * 
 * @author philippe
 */
public class AddTodayDateProcessor implements IAttributesProcessor {

    public static final String DATE_ATTRIBUTE_NAME = "date";

    @Override
    public void processAttributes(final Map<String, List<Object>> attributes) {

        final ArrayList<Object> dateAttribute = new ArrayList<Object>(1);
        dateAttribute.add(new Date().toString());
        attributes.put(DATE_ATTRIBUTE_NAME, dateAttribute);

    }

    @Override
    public Set<String> getPossibleUserAttributeNames() {

        HashSet<String> newAttributes = new HashSet<String>(1);
        newAttributes.add(DATE_ATTRIBUTE_NAME);
        return newAttributes;
    }

}
