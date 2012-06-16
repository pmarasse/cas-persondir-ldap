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

import javax.naming.directory.SearchControls;

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
import org.springframework.ldap.core.LdapTemplate;

public class LdapPersonAttributeDao implements IPersonAttributeDao, InitializingBean {

    final Logger                  log                    = LoggerFactory.getLogger(LdapPersonAttributeDao.class);

    final static Pattern          QUERY_PLACEHOLDER      = Pattern.compile("\\{0\\}");

    /**
     * LDAP Context Source used to query the directory
     */
    protected ContextSource       contextSource;

    /**
     * Simple mapping from LDAP Attribute names (keys), to expected Attribute names (values)
     */
    protected Map<String, String> resultAttributeMapping = new HashMap<String, String>();

    /**
     * ldapFilter used to retrieve one person.
     */
    protected String              ldapFilter             = "(uid={0})";

    /**
     * Base DN for LDAP query
     */
    protected String              baseDN                 = "";

    /**
     * List of queried attributes (usually not mapped)
     */
    protected List<String>        queriedAttributes      = new ArrayList<String>();

    /**
     * Ldap template used to query the directory
     */
    private LdapTemplate          ldapTemplate;

    /**
     * Ldap Search controls used internally
     */
    private SearchControls        sc                     = new SearchControls();

    /**
     * Set of attributes to query (mapped and raw attributes) used internally
     */
    private HashSet<String>       queriedAttributesSet;

    /**
     * List of attributes processors
     */
    List<IAttributesProcessor>     processors             = new ArrayList<IAttributesProcessor>();

    // Implements InitializingBean

    @Override
    public synchronized void afterPropertiesSet() throws Exception {

        if (contextSource == null) {
            throw new BeanCreationException("LDAP contextSource cannot be null");
        }
        ldapTemplate = new LdapTemplate(contextSource);
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

        if (log.isDebugEnabled()) {
            log.debug("Attributes queried : " + Arrays.toString(attrs));
        }
        sc.setReturningAttributes(attrs);

    }

    // Implements IPersonAttributeDao Interface

    @Override
    public IPersonAttributes getPerson(String uid) {

        Matcher queryMatcher = QUERY_PLACEHOLDER.matcher(ldapFilter);
        String localFilter = queryMatcher.replaceAll(uid);

        if (log.isDebugEnabled()) {
            log.debug("SearchFilter : " + localFilter);
        }

        try {
            @SuppressWarnings("unchecked")
            List<IPersonAttributes> resultList = ldapTemplate.search(baseDN, localFilter, sc, new PersonAttributeMapper(uid));

            if (resultList.isEmpty()) {
                return null;
            }
            return resultList.get(0);

        } catch (NameNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage());
            }
            return null;
        }
    }

    /**
     * As this implementation does not support parametrized query other than
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

        Set<String> attributesToReturn = new HashSet<String>();

        while (iter.hasNext()) {
            sourceAttributeName = iter.next();
            if ((targetAttributeName = resultAttributeMapping.get(sourceAttributeName)) != null) {
                iter.remove();
                attributesToReturn.add(targetAttributeName);
            } else {
                attributesToReturn.add(sourceAttributeName);
            }
        }

        for (IAttributesProcessor processor : processors) {
            attributesToReturn.addAll(processor.getPossibleUserAttributeNames());
        }
        
        return attributesToReturn;

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

    public String getBaseDN() {

        return baseDN;
    }

    public void setBaseDN(String baseDN) {

        if (baseDN == null) {
            throw new IllegalArgumentException("baseDN cannot be null");
        }

        this.baseDN = baseDN;
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
            Map<String, List<Object>> personAttrsMap = new HashMap<String, List<Object>>();
            String targetAttribute;

            for (String attribute : queriedAttributesSet) {
                Object[] values = context.getObjectAttributes(attribute);
                if (values != null) {
                    if ((targetAttribute = resultAttributeMapping.get(attribute)) == null) {
                        personAttrsMap.put(attribute, Arrays.asList(values));
                    } else {
                        personAttrsMap.put(targetAttribute, Arrays.asList(values));
                    }
                }
            }
            return new PersonAttributesImpl(uid, personAttrsMap);
        }
    }

}
