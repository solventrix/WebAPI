package com.jnj.honeur.webapi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Resolves to the correct data source id based on the source daimon context of the running thread
 *
 * @author Peter Moorthamer
 * Date: 02/feb/2018
 */
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    private static final Log LOG = LogFactory.getLog(CurrentTenantIdentifierResolverImpl.class);

    private String defaultDataSourceKey;

    public CurrentTenantIdentifierResolverImpl(final String defaultDataSourceKey) {
        this.defaultDataSourceKey = defaultDataSourceKey;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String currentTenant = SourceDaimonContextHolder.getCurrentSourceDaimonContextKey();
        if(currentTenant != null) {
            LOG.trace("Tenant resolved: " + currentTenant);
            return currentTenant;
        }
        LOG.trace("Resolved to default tenant: " + defaultDataSourceKey);
        return defaultDataSourceKey;
    }

    public void setTenant(String tenant) {
        SourceDaimonContextHolder.setCurrentSourceDaimonContextKey(tenant);
    }

    public void onDestroy() {
        SourceDaimonContextHolder.clear();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

}
