package net.archigny.cas.persondir.processors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;

public class UuidBinaryToStringProcessor implements IAttributesProcessor, InitializingBean {

    private Logger    log                         = LoggerFactory.getLogger(UuidBinaryToStringProcessor.class);

    /**
     * Name of the attribute where binary data of UUID will be found.
     */
    private String    sourceAttribute;

    /**
     * Name of the attribute where String representation of UUID will be written
     */
    private String    targetAttribute;

    /**
     * Specify if an IllegalFormatException will be raised if input data of UUID is invalid (eg not 128bit long)
     */
    private boolean   raiseIllegalFormatException = false;

    /**
     * True if source attribute is deleted when conversion is done.
     */
    private boolean   deleteSourceAttribute       = true;

    /**
     * UUID length in bytes
     */
    public static int UUID_LENGTH                 = 16;

    @Override
    public void afterPropertiesSet() throws Exception {

        if (sourceAttribute == null) {
            throw new BeanCreationException("sourceAttribute cannot be null");
        }
        if (targetAttribute == null) {
            throw new BeanCreationException("targetAttribute cannot be null");
        }
        if (log.isDebugEnabled()) {
            log.debug("Bean validated, will " + (raiseIllegalFormatException ? "" : "not")
                    + " raise an exception on illegal UUID format, and " + (deleteSourceAttribute ? "delete" : "preserve")
                    + " source attribute");
        }
    }

    @Override
    public void processAttributes(Map<String, List<Object>> attributes) {

        List<Object> source = attributes.get(sourceAttribute);

        if ((source == null) || (source.isEmpty())) {
            return;
        }

        List<Object> target = new ArrayList<Object>();

        for (Object object : source) {
            try {
                if (object instanceof String) {
                    target.add(UuidToString(((String) object).getBytes()));
                } else {
                    target.add(UuidToString((byte[]) object));
                }
            } catch (ClassCastException e) {
                log.error("Unable to cast : " + object.getClass().getCanonicalName() + " to byte[]");
                if (raiseIllegalFormatException) {
                    throw new IllegalArgumentException("object : " + object.toString() + " cannot be casted to byte[]");
                }
                target.add("Error");
            }
        }

        if (deleteSourceAttribute) {
            attributes.remove(sourceAttribute);
        }

        attributes.put(targetAttribute, target);

    }

    @Override
    public Set<String> getPossibleUserAttributeNames() {

        HashSet<String> attributeNames = new HashSet<String>();

        attributeNames.add(targetAttribute);

        return attributeNames;
    }

    private String UuidToString(byte[] UuidBytes) {

        if (UuidBytes.length != UUID_LENGTH) {
            log.error("UUID hase illegal length : " + UuidBytes.length + " expected : " + UUID_LENGTH);
            if (raiseIllegalFormatException) {
                throw new IllegalArgumentException("Uuid passed is not " + UUID_LENGTH + " bytes long !");
            }
            return "Error";
        }

        // Prepare the buffer
        StringBuilder UuidString = new StringBuilder(UUID_LENGTH * 2 + 4);
        for (int i = 0; i < UUID_LENGTH; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                UuidString.append("-");
            }
            UuidString.append(String.format("%02x", UuidBytes[i]));
        }

        return UuidString.toString().toUpperCase();

    }

    // Getters and setters

    public String getSourceAttribute() {

        return sourceAttribute;
    }

    public void setSourceAttribute(String sourceAttribute) {

        this.sourceAttribute = sourceAttribute;
    }

    public String getTargetAttribute() {

        return targetAttribute;
    }

    public void setTargetAttribute(String targetAttribute) {

        this.targetAttribute = targetAttribute;
    }

    public boolean isRaiseIllegalFormatException() {

        return raiseIllegalFormatException;
    }

    public void setRaiseIllegalFormatException(boolean raiseIllegalFormatException) {

        this.raiseIllegalFormatException = raiseIllegalFormatException;
    }

    public boolean isDeleteSourceAttribute() {

        return deleteSourceAttribute;
    }

    public void setDeleteSourceAttribute(boolean deleteSourceAttribute) {

        this.deleteSourceAttribute = deleteSourceAttribute;
    }

}
