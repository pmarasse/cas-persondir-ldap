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

import org.apereo.services.persondir.IPersonAttributeDao;
import org.apereo.services.persondir.IPersonAttributes;
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
    protected LdapName                   contextSourceBaseDN;

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

    /**
     * True if attribute fetching has to be done by direct reading of user DN : a first query is done to find the user DN, then a
     * second query is done to fetch attributes. Some directories need that to fetch constructed attributes (eg: tokenGroups on
     * Active Directory)
     */
    private boolean                      fetchDirectDn                = false;

    // Implements InitializingBean

    /**
     * "Cast" a string Set to an Array (toArray seems not to work as expected)
     * 
     * @param dataSet
     *            Set to convert
     * @return array of data
     */
    protected String[] stringSetToArray(final Set<String> dataSet) {

        if (dataSet == null) {
            return null;
        }

        // Cast Set to String Array... cannot do this by .toArray() ??
        final String[] dataArray = new String[dataSet.size()];
        int i = 0;
        for (String data : dataSet) {
            dataArray[i++] = data;
        }
        return dataArray;
    }

    @Override
    public synchronized void afterPropertiesSet() throws Exception {

        if (contextSource == null) {
            throw new BeanCreationException("LDAP contextSource cannot be null");
        }
        ldapTemplate = new LdapTemplate(contextSource);
        ldapTemplate.setIgnorePartialResultException(ignorePartialResultException);

        queriedAttributesSet = new HashSet<String>(queriedAttributes.size());
        queriedAttributesSet.addAll(queriedAttributes);
        queriedAttributesSet.addAll(resultAttributeMapping.keySet());

        // Setting SearchControls
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningObjFlag(true);
        if (fetchDirectDn) {
            String[] attrs = new String[1];
            attrs[0] = "dn";
            sc.setReturningAttributes(attrs);
        } else {
            String[] attrs = stringSetToArray(queriedAttributesSet);
            sc.setReturningAttributes(attrs);
        }

        if (log.isDebugEnabled()) {
            log.debug("afterPropertiesSet, set attributes to be queried : "
                    + Arrays.toString(stringSetToArray(queriedAttributesSet))
                    + (dnAttributeName == null ? ", no DN attribute created" : ", will add attribute [" + dnAttributeName
                            + "] to store user DN"));
        }

        if (baseDN == null) {
            baseDN = new LdapName("");
        }

    }

    // Implements IPersonAttributeDao Interface

    @SuppressWarnings("unchecked")
    @Override
    public ILockablePersonAttributes getPerson(final String uid) {

        final Matcher queryMatcher = QUERY_PLACEHOLDER.matcher(ldapFilter);
        final String localFilter = queryMatcher.replaceAll(uid);

        if (log.isDebugEnabled()) {
            log.debug("getPerson SearchFilter : {}", localFilter);
            log.debug("getPerson Attributes queried : {}", Arrays.toString(sc.getReturningAttributes()));
        }

        try {

            final ILockablePersonAttributes result;

            if (fetchDirectDn) {
                List<String> userDN = ldapTemplate.search(baseDN, localFilter, sc, new DnFetcher());
                if (userDN.isEmpty()) {
                    return null;
                }
                if (log.isDebugEnabled()) {
                    log.debug("user DN found : " + userDN.get(0) + " fetching attributes");
                }
                result = (ILockablePersonAttributes) ldapTemplate.lookup(userDN.get(0), stringSetToArray(queriedAttributesSet),
                        new PersonAttributeMapper(uid));

            } else {
                // Fetch person from directory
                List<ILockablePersonAttributes> resultList = ldapTemplate.search(baseDN, localFilter, sc,
                        new PersonAttributeMapper(uid));
                if (resultList.isEmpty()) {
                    return null;
                }
                result = resultList.get(0);
            }

            // Process attributes if needed
            if (result != null) {
                final Map<String, List<Object>> attrs = result.getAttributes();
                for (IAttributesProcessor processor : processors) {
                    processor.processAttributes(attrs);
                }
            }
            // Lock the result before returning it
            result.lock();
            return result;

        } catch (NameNotFoundException e) {
            log.debug("Catched while retrieving result : {}", e.getMessage());
            return null;
        }
    }

    /**
     * As this implementation does not support parametrized query other than simple ldap filter
     */
    @Override
    public Set<IPersonAttributes> getPeople(final Map<String, Object> query) {

        return null;
    }

    /**
     * Method not implemented... so returning null.
     */
    @Override
    public Set<IPersonAttributes> getPeopleWithMultivaluedAttributes(final Map<String, List<Object>> query) {

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
            log.debug("possible AttributeNames Set before processors : {}", Arrays.toString(possibleAttributenames.toArray()));
        }
        for (IAttributesProcessor processor : processors) {
            final Set<String> processorAttributeNames = processor.getPossibleUserAttributeNames();
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
            log.debug("returning AttributeNames Set : {} ", Arrays.toString(possibleAttributenames.toArray()));
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
    public Map<String, List<Object>> getMultivaluedUserAttributes(final Map<String, List<Object>> seed) {

        throw new UnsupportedOperationException("This method is deprecated and not implemented in this class");
    }

    /**
     * As this method is deprecated, it throws an UnsupportedOperationException
     */
    @Override
    @Deprecated
    public Map<String, List<Object>> getMultivaluedUserAttributes(final String uid) {

        throw new UnsupportedOperationException("This method is deprecated and not implemented in this class");
    }

    /**
     * As this method is deprecated, it throws an UnsupportedOperationException
     */
    @Override
    @Deprecated
    public Map<String, Object> getUserAttributes(final Map<String, Object> seed) {

        throw new UnsupportedOperationException("This method is deprecated and not implemented in this class");
    }

    /**
     * As this method is deprecated, it throws an UnsupportedOperationException
     */
    @Override
    @Deprecated
    public Map<String, Object> getUserAttributes(final String uid) {

        throw new UnsupportedOperationException("This method is deprecated and not implemented in this class");
    }

    @Override
    public int compareTo(IPersonAttributeDao o) {
    
        // TODO Auto-generated method stub
        return 0;
    }

    // Setters and getters

    public ContextSource getContextSource() {

        return contextSource;
    }

    public void setContextSource(final ContextSource contextSource) {

        if (contextSource == null) {
            throw new IllegalArgumentException("contextSource cannot be null");
        }

        this.contextSource = contextSource;
    }

    public Map<String, String> getResultAttributeMapping() {

        return resultAttributeMapping;
    }

    public void setResultAttributeMapping(final Map<String, String> resultAttributeMapping) {

        if (resultAttributeMapping == null) {
            throw new IllegalArgumentException("resultAttributeMapping cannot be null");
        }

        this.resultAttributeMapping = resultAttributeMapping;
    }

    public String getLdapFilter() {

        return ldapFilter;
    }

    public void setLdapFilter(final String ldapFilter) {

        if (ldapFilter == null) {
            throw new IllegalArgumentException("ldapFilter cannot be null");
        }

        this.ldapFilter = ldapFilter;
    }

    // Wrapper getter around LdapName
    public String getBaseDN() {

        return (baseDN == null ? "" : baseDN.toString());
    }

    public void setBaseDN(final String baseDN) {

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

    public void setContextSourceBaseDN(final String contextSourceBaseDN) {

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

    public void setQueriedAttributes(final List<String> queriedAttributes) {

        if (queriedAttributes == null) {
            throw new IllegalArgumentException("queriedAttributes cannot be null");
        }

        this.queriedAttributes = queriedAttributes;
    }

    public List<IAttributesProcessor> getProcessors() {

        return processors;
    }

    public void setProcessors(final List<IAttributesProcessor> processors) {

        if (processors == null) {
            throw new IllegalArgumentException("processors list cannot be null");
        }
        this.processors = processors;
    }

    public String getDnAttributeName() {

        return dnAttributeName;
    }

    public void setDnAttributeName(final String dnAttributeName) {

        this.dnAttributeName = dnAttributeName;
    }

    public boolean isIgnorePartialResultException() {

        return ignorePartialResultException;
    }

    public void setIgnorePartialResultException(final boolean ignorePartialResultException) {

        this.ignorePartialResultException = ignorePartialResultException;
    }

    public boolean isFetchDirectDn() {

        return fetchDirectDn;
    }

    public void setFetchDirectDn(final boolean fetchDirectDn) {

        this.fetchDirectDn = fetchDirectDn;
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

            final DirContextAdapter context = (DirContextAdapter) ctx;
            if (log.isDebugEnabled()) {
                log.debug("Attributes returned by context : {}", context.getAttributes().toString());
                log.debug("Processing queried attributes : {}", Arrays.toString(queriedAttributesSet.toArray()));
            }
            final Map<String, List<Object>> personAttrsMap = new HashMap<String, List<Object>>();
            String targetAttribute;
            List<Object> valuesToAdd;

            if (dnAttributeName != null) {

                String objectDNString = context.getDn().toString();

                if (contextSourceBaseDN != null) {

                    final DistinguishedName objectDN = new DistinguishedName(contextSourceBaseDN);

                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Trying to add {}  to {}", context.getDn().toString(), objectDN.toString());
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

                final List<Object> valuesDN = new ArrayList<Object>(1);
                valuesDN.add(objectDNString);
                personAttrsMap.put(dnAttributeName, valuesDN);
            }

            for (final String attribute : queriedAttributesSet) {
                Object[] values = context.getObjectAttributes(attribute);
                if (log.isDebugEnabled()) {
                    log.debug("Attribute : {}  values : {}", attribute, Arrays.toString(values));
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

    /**
     * A simple ContextMapper to only fetch DN of result objects.
     * 
     * @author Philippe Marasse <philippe.marasse@laposte.net>
     */
    protected class DnFetcher implements ContextMapper {

        @Override
        public Object mapFromContext(final Object ctx) {

            final DirContextAdapter context = (DirContextAdapter) ctx;
            if (log.isDebugEnabled()) {
                log.debug("Attributes returned by context : {}", context.getAttributes().toString());
            }

            if (contextSourceBaseDN == null) {
                return context.getDn().toString();
            }

            final DistinguishedName dn = new DistinguishedName(contextSourceBaseDN);

            if (dn.isEmpty()) {
                return context.getDn().toString();
            } else {
                try {
                    dn.addAll(context.getDn());
                    return dn.toString();
                } catch (InvalidNameException e) {
                    // Unexpected... in this case, return only context DN
                    return context.getDn().toString();
                }
            }

        }

    }
}
