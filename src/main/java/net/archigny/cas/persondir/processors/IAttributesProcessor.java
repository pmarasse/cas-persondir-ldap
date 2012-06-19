package net.archigny.cas.persondir.processors;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple Interface for attributes processors.
 * 
 * @author Philippe Marasse <philippe@archigny.net>
 * 
 */
public interface IAttributesProcessor {

    /**
     * Processes the provided attributes
     * 
     * @param attributes
     *            attributes to be processed
     */
    public void processAttributes(final Map<String, List<Object>> attributes);

    /**
     * Get the set of attributes added by this processor
     * 
     * @return Set of the attrbutes names that can be added by the processor - null or empty set if no name is added
     */
    public Set<String> getPossibleUserAttributeNames();

}
