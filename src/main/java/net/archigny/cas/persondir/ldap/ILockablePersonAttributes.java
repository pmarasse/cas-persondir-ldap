package net.archigny.cas.persondir.ldap;

import org.apereo.services.persondir.IPersonAttributes;


public interface ILockablePersonAttributes extends IPersonAttributes {

    /**
     * Method used to lock Attributes preventing further modifications. 
     */
    public void lock();
    
    /**
     * Query implementation to know if attributes are read-only locked.
     * 
     * @return true if Attributes are locked (read-only)
     */
    boolean isLocked();
    
}
