package net.archigny.cas.persondir.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InvalidNameException;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapName;

import net.archigny.cas.persondir.processors.IAttributesProcessor;

import org.jasig.services.persondir.IPersonAttributeDao;
import org.jasig.services.persondir.IPersonAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;

public class LdapPersonAttributeDao implements IPersonAttributeDao, InitializingBean {

    private final Logger                 log                          = LoggerFactory.getLogger(LdapPersonAttributeDao.class);

    protected final static Pattern       QUERY_PLACEHOLDER            = Pattern.compile("\\{0\\}");

    /**
     * LDAP Context Source used to query the directory
     */
    protected ContextSource              contextSource;

    /**
     * base DN provided to ContextSource
     */
    protected LdapName 					 contextSourceBaseDN;
    
    /**
     * Simple mapping from LDAP Attribute names (keys), to expected Attribute names (values)
     */
    protected Map<String, String>        resultAttributeMapping       = new HashMap<String, String>();

    /**
     * ldapFilter used to retrieve one person.
     */
    protected String                     ldapFilter                   = "(uid={0})";

    /**
     * Base DN for LDAP query
     */
    protected LdapName                   baseDN;

    /**
     * List of queried attributes (usually not mapped)
     */
    protected List<String>               queriedAttributes            = new ArrayList<String>();

    /**
     * Ldap template used to query the directory
     */
    private LdapTemplate                 ldapTemplate;

    /**
     * Ldap Search controls used internally
     */
    private SearchControls               sc                           = new SearchControls();

    /**
     * Set of attributes to query (mapped and raw attributes) used internally
     */
    private HashSet<String>              queriedAttributesSet;

    /**
     * List of attributes processors
     */
    protected List<IAttributesProcessor> processors                   = new ArrayList<IAttributesProcessor>();

    /**
     * Flag set for PartialResultException that can be raised when querying AD with its root DN as base
     */
    private boolean                      ignorePartialResultException = true;

    /**
     * Name of the attribute used to store the Distinguished Name (null => no storage)
     */
    private String                       dnAttributeName;
    
    // Implements InitializingBean

    @Override
    public synchronized void afterPropertiesSet() throws Exception {

        if (contextSource == null) {
            throw new BeanCreationException("LDAP contextSource cannot be null");
        }
        ldapTemplate = new LdapTemplate(contextSource);        
        ldapTemplate.setIgnorePartialResultException(ignorePartialResultException);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningObjFlag(true);

        queriedAttributesSet = new HashSet<String>(queriedAttributes.size());
        queriedAttributesSet.addAll(queriedAttributes);
        queriedAttributesSet.addAll(resultAttributeMapping.keySet());

        // Cast Set to String Array... cannot do this by .toArray() ??
        String[] attrs = new String[queriedAttributesSet.size()];
        int i = 0;
        for (String attribute : queriedAttributesSet) {
            attrs[i++] = attribute;
        }

        sc.setReturningAttributes(attrs);
        if (log.isDebugEnabled()) {
            log.debug("afterPropertiesSet, set attributes to be queried : "
                    + Arrays.toString(attrs)
                    + (dnAttributeName == null ? ", no DN attribute created" : ", will add attribute [" + dnAttributeName
                            + "] to store user DN"));
        }

        if (baseDN == null) {
            baseDN = new LdapName("");
        }

    }

    // Implements IPersonAttributeDao Interface

    @Override
    public ILockablePersonAttributes getPerson(String uid) {

        Matcher queryMatcher = QUERY_PLACEHOLDER.matcher(ldapFilter);
        String localFilter = queryMatcher.replaceAll(uid);

        if (log.isDebugEnabled()) {
            log.debug("getPerson SearchFilter : " + localFilter);
            log.debug("getPerson Attributes queried : " + Arrays.toString(sc.getReturningAttributes()));
        }

        try {
            // Fetch person from directory
            @SuppressWarnings("unchecked")
            List<ILockablePersonAttributes> resultList = ldapTemplate.search(baseDN, localFilter, sc,
                    new PersonAttributeMapper(uid));

            if (resultList.isEmpty()) {
                return null;
            }
            // Process attributes if needed
            ILockablePersonAttributes result = resultList.get(0);
            if (result != null) {
                Map<String, List<Object>> attrs = result.getAttributes();
                for (IAttributesProcessor processor : processors) {
                    processor.processAttributes(attrs);
                }
            }
            // Lock the result before returning it
            result.lock();
            return result;

        } catch (NameNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage());
            }
            return null;
        }
    }

    /**
     * As this implementation does not support parametrized query other than simple ldap filter
     */
    @Override
    public Set<IPersonAttributes> getPeople(Map<String, Object> query) {

        return null;
    }

    /**
     * Method not implemented... so returning null.
     */
    @Override
    public Set<IPersonAttributes> getPeopleWithMultivaluedAttributes(Map<String, List<Object>> query) {

        return null;
    }

    /**
     * Return a list of returned attributes.
     */
    @Override
    public Set<String> getPossibleUserAttributeNames() {

        // Process attribute names to map them
        String sourceAttributeName;
        String targetAttributeName;

        Iterator<String> iter = queriedAttributesSet.iterator();

        Set<String> possibleAttributenames = new HashSet<String>();

        while (iter.hasNext()) {
            sourceAttributeName = iter.next();
            if ((targetAttributeName = resultAttributeMapping.get(sourceAttributeName)) != null) {
                possibleAttributenames.add(targetAttributeName);
            } else {
                possibleAttributenames.add(sourceAttributeName);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("possible AttributeNames Set before processors : " + Arrays.toString(possibleAttributenames.toArray()));
        }
        for (IAttributesProcessor processor : processors) {
            Set<String> processorAttributeNames = processor.getPossibleUserAttributeNames();
            if (processorAttributeNames != null) {
                possibleAttributenames.addAll(processorAttributeNames);
            }
        }
        if (dnAttributeName != null) {
            if (possibleAttributenames.contains(dnAttributeName)) {
                log.warn("dnAttributeName collides with existing attribute in queriedAttributes, resultAttributeMapping or processor");
            } else {
                possibleAttributenames.add(dnAttributeName);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("returning AttributeNames Set : " + Arrays.toString(possibleAttributenames.toArray()));
        }

        return possibleAttributenames;

    }

    /**
     * As the LDAP filter is provided, this implementation cannot provides the set of supported query attributes : returning null.
     */
    @Override
    public Set<String> getAvailableQueryAttributes() {

        return null;
    }

    /**
     * As this method is deprecated, it throws an UnsupportedOperationException
     */
    @Override
    @Deprecated
    public Map<String, List<Object>> getMultivaluedUserAttributes(Map<String, List<Object>> seed) {

        throw new UnsupportedOperationException("This method is deprecated and not implemented in this class");
    }

    /**
     * As this method is deprecated, it throws an UnsupportedOperationException
     */
    @Override
    @Deprecated
    public Map<String, List<Object>> getMultivaluedUserAttributes(String uid) {

        throw new UnsupportedOperationException("This method is deprecated and not implemented in this class");
    }

    /**
     * As this method is deprecated, it throws an UnsupportedOperationException
     */
    @Override
    @Deprecated
    public Map<String, Object> getUserAttributes(Map<String, Object> seed) {

        throw new UnsupportedOperationException("This method is deprecated and not implemented in this class");
    }

    /**
     * As this method is deprecated, it throws an UnsupportedOperationException
     */
    @Override
    @Deprecated
    public Map<String, Object> getUserAttributes(String uid) {

        throw new UnsupportedOperationException("This method is deprecated and not implemented in this class");
    }

    // Setters and getters

    public ContextSource getContextSource() {

        return contextSource;
    }

    public void setContextSource(ContextSource contextSource) {

        if (contextSource == null) {
            throw new IllegalArgumentException("contextSource cannot be null");
        }

        this.contextSource = contextSource;
    }

    public Map<String, String> getResultAttributeMapping() {

        return resultAttributeMapping;
    }

    public void setResultAttributeMapping(Map<String, String> resultAttributeMapping) {

        if (resultAttributeMapping == null) {
            throw new IllegalArgumentException("resultAttributeMapping cannot be null");
        }

        this.resultAttributeMapping = resultAttributeMapping;
    }

    public String getLdapFilter() {

        return ldapFilter;
    }

    public void setLdapFilter(String ldapFilter) {

        if (ldapFilter == null) {
            throw new IllegalArgumentException("ldapFilter cannot be null");
        }

        this.ldapFilter = ldapFilter;
    }

    // Wrapper getter around LdapName
    public String getBaseDN() {

        return (baseDN == null ? "" : baseDN.toString());
    }

    public void setBaseDN(String baseDN) {

        if (baseDN == null) {
            throw new IllegalArgumentException("baseDN cannot be null");
        }
        try {
            this.baseDN = new LdapName(baseDN);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException(e);
        }

    }

    // Wrapper getter around LdapName
    public String getContextSourceBaseDN() {

        return (contextSourceBaseDN == null ? "" : contextSourceBaseDN.toString());
    }

    public void setContextSourceBaseDN(String contextSourceBaseDN) {

        if (contextSourceBaseDN == null) {
            throw new IllegalArgumentException("contextSourceBaseDN cannot be null");
        }
        try {
            this.contextSourceBaseDN = new LdapName(contextSourceBaseDN);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException(e);
        }

    }

    public List<String> getQueriedAttributes() {

        return queriedAttributes;
    }

    public void setQueriedAttributes(List<String> queriedAttributes) {

        if (queriedAttributes == null) {
            throw new IllegalArgumentException("queriedAttributes cannot be null");
        }

        this.queriedAttributes = queriedAttributes;
    }

    public List<IAttributesProcessor> getProcessors() {

        return processors;
    }

    public void setProcessors(List<IAttributesProcessor> processors) {

        if (processors == null) {
            throw new IllegalArgumentException("processors list cannot be null");
        }
        this.processors = processors;
    }

    public String getDnAttributeName() {

        return dnAttributeName;
    }

    public void setDnAttributeName(String dnAttributeName) {

        this.dnAttributeName = dnAttributeName;
    }

    public boolean isIgnorePartialResultException() {

        return ignorePartialResultException;
    }

    public void setIgnorePartialResultException(boolean ignorePartialResultException) {

        this.ignorePartialResultException = ignorePartialResultException;
    }

    /**
     * Private DAO class which creates personAttributes from LDAP response
     * 
     * @author Philippe Marasse <philippe.marasse@laposte.net>
     * 
     */
    private class PersonAttributeMapper implements ContextMapper {

        private String uid;

        public PersonAttributeMapper(String uid) {

            this.uid = uid;
        }

        @Override
        public Object mapFromContext(Object ctx) {

            DirContextAdapter context = (DirContextAdapter) ctx;
            if (log.isDebugEnabled()) {
                log.debug("Attributes returned by context : " + context.getAttributes().toString());
                log.debug("Processing queried attributes : " + Arrays.toString(queriedAttributesSet.toArray()));
            }
            Map<String, List<Object>> personAttrsMap = new HashMap<String, List<Object>>();
            String targetAttribute;
            List<Object> valuesToAdd;

            if (dnAttributeName != null) {

                DistinguishedName objectDN = new DistinguishedName(contextSourceBaseDN);
                String objectDNString;

                if (objectDN.isEmpty()) {

                    objectDNString = context.getDn().toString();
                } else {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Trying to add " + context.getDn().toString() + " to " + objectDN.toString());
                        }
                        objectDN.addAll(context.getDn());
                        objectDNString = objectDN.toString();
                    } catch (InvalidNameException e) {
                        // Unexpected... in this case, return only context DN
                        objectDNString = context.getDn().toString();
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("Adding computed DN attribute name [" + dnAttributeName + "], value [" + objectDNString + "]");
                }

                List<Object> valuesDN = new ArrayList<Object>(1);
                valuesDN.add(objectDNString);
                personAttrsMap.put(dnAttributeName, valuesDN);
            }

            for (String attribute : queriedAttributesSet) {
                Object[] values = context.getObjectAttributes(attribute);
                if (log.isDebugEnabled()) {
                    log.debug("Attribute : " + attribute + " values : " + Arrays.toString(values));
                }
                if (values != null) {
                    valuesToAdd = new ArrayList<Object>();
                    valuesToAdd.addAll(Arrays.asList(values));
                    if ((targetAttribute = resultAttributeMapping.get(attribute)) == null) {
                        personAttrsMap.put(attribute, valuesToAdd);
                    } else {
                        personAttrsMap.put(targetAttribute, valuesToAdd);
                    }
                }
            }
            return new PersonAttributesImpl(uid, personAttrsMap);
        }
    }

}
