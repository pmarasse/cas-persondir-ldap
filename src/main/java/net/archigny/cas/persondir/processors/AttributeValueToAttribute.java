package net.archigny.cas.persondir.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;

import net.archigny.cas.persondir.processors.IAttributesProcessor;

/**
 * Attribute processor that can map some values of a multi-valued attribute (typically groups) to a new attribute. Values matches
 * with a fixed prefix.
 * 
 * @author Philippe Marasse <philippe.marasse@ch-poitiers.fr>
 */
public class AttributeValueToAttribute implements IAttributesProcessor, InitializingBean {

    private static Logger       log                = LoggerFactory.getLogger(AttributeValueToAttribute.class);

    /**
     * Name of the attribute to process
     */
    private String              attributeName;

    /**
     * Map between prefixes and new attribute names
     */
    private Map<String, String> prefixToAttributeName;

    /**
     * True if attribute value is moved to new attribute (false : a copy is done)
     */
    private boolean             moveAttributeValue = true;

    @Override
    public void afterPropertiesSet() throws Exception {

        if (attributeName == null) {
            throw new BeanCreationException("attributeName cannot be null");
        }
        if (prefixToAttributeName == null) {
            throw new BeanCreationException("prefixToAttributeName hashmap cannot be null");
        }
        if (prefixToAttributeName.containsKey(attributeName)) {
            throw new BeanCreationException("attributeName scanned cannot be in new attributes");
        }
        log.debug("Bean values validated.");
    }

    // Méthodes de processeur

    @Override
    public void processAttributes(final Map<String, List<Object>> attributes) {

        final boolean debugMode = log.isDebugEnabled();

        if (attributes.containsKey(attributeName)) {
            List<Object> attributesValues = attributes.get(attributeName);
            if (debugMode) {
                log.debug("attribute {} found. Processing values : {}", attributeName, Arrays.toString(attributesValues.toArray()));
            }

            // Crée un hash des nouveaux attributs potentiels
            Map<String, List<Object>> newAttributes = new HashMap<String, List<Object>>();
            for (String newAttribute : prefixToAttributeName.values()) {
                if (!newAttributes.containsKey(newAttribute)) {
                    newAttributes.put(newAttribute, new ArrayList<Object>());
                }
            }
            if (debugMode) {
                log.debug("New attributes map : {}", Arrays.toString(newAttributes.keySet().toArray()));
            }

            final List<Object> valuesToRemove = new ArrayList<Object>();

            // Parcourt la liste des valeurs
            for (Object value : attributesValues) {
                log.debug("considering value : {}", value);

                // Parcourt chaque préfixe
                for (final String prefix : prefixToAttributeName.keySet()) {

                    if (((String) value).startsWith(prefix)) {
                        // Ajoute la valeur (sans le préfixe), dans le nouvel
                        // attribut
                        newAttributes.get(prefixToAttributeName.get(prefix)).add(((String) value).substring(prefix.length()));

                        if (moveAttributeValue) {
                            valuesToRemove.add(value);
                        }

                        if (debugMode) {
                            log.debug("Value match prefix " + prefix + (moveAttributeValue ? " moved" : " copied")
                                    + " to attribute " + prefixToAttributeName.get(prefix));
                        }
                    }
                }
            }

            // Maintenant on transfère les nouveaux attributs s'ils ne sont pas
            // vides
            for (String newAttributeName : newAttributes.keySet()) {
                List<Object> newAttributeValues = newAttributes.get(newAttributeName);
                if (!newAttributeValues.isEmpty()) {
                    attributes.put(newAttributeName, newAttributeValues);
                    if (debugMode) {
                        log.debug("Attribute added : {} with values {}", newAttributeName,
                                Arrays.toString(newAttributeValues.toArray()));
                    }
                }
            }

            if (valuesToRemove.isEmpty()) {
                log.debug("No attribute values to remove");
            } else {
                if (debugMode) {
                    log.debug("Will remove values : {}", Arrays.toString(valuesToRemove.toArray()));
                }
                try {
                    // RemoveAll seems to trigger a strange exception
                    attributesValues.removeAll(valuesToRemove);
                } catch (UnsupportedOperationException e) {
                    for (Object valueToRemove : valuesToRemove) {
                        try {
                            attributesValues.remove(attributesValues.indexOf(valueToRemove));
                        } catch (UnsupportedOperationException e2) {
                            log.error("attributesValues list ({}) does not implements remove operation!", attributesValues
                                    .getClass().getCanonicalName());
                            break;
                        }
                    }
                }
                if (debugMode) {
                    log.debug("Remaining values of {} : {}", attributeName, Arrays.toString(attributesValues.toArray()));
                }
            }
        }

    }

    @Override
    public Set<String> getPossibleUserAttributeNames() {

        final Set<String> possibleAttributeNames = new HashSet<String>(prefixToAttributeName.values().size());
        possibleAttributeNames.addAll(prefixToAttributeName.values());
        if (log.isDebugEnabled()) {
            log.debug("possible attribute names returned : {}", Arrays.toString(possibleAttributeNames.toArray()));
        }
        return possibleAttributeNames;
    }

    // Getters and setters

    public String getAttributeName() {

        return attributeName;
    }

    public void setAttributeName(final String attributeName) {

        this.attributeName = attributeName;
    }

    public Map<String, String> getPrefixToAttributeName() {

        return prefixToAttributeName;
    }

    public void setPrefixToAttributeName(final Map<String, String> prefixToAttributeName) {

        this.prefixToAttributeName = prefixToAttributeName;
    }

    public boolean isMoveAttributeValue() {

        return moveAttributeValue;
    }

    public void setMoveAttributeValue(final boolean moveAttributeValue) {

        this.moveAttributeValue = moveAttributeValue;
    }

}
