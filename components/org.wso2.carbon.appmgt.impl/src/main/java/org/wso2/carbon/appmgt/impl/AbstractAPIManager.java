/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.appmgt.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.appmgt.api.APIManager;
import org.wso2.carbon.appmgt.api.AppManagementException;
import org.wso2.carbon.appmgt.api.AppMgtAuthorizationFailedException;
import org.wso2.carbon.appmgt.api.AppMgtResourceAlreadyExistsException;
import org.wso2.carbon.appmgt.api.AppMgtResourceNotFoundException;
import org.wso2.carbon.appmgt.api.model.APIIdentifier;
import org.wso2.carbon.appmgt.api.model.APIKey;
import org.wso2.carbon.appmgt.api.model.Icon;
import org.wso2.carbon.appmgt.api.model.Subscriber;
import org.wso2.carbon.appmgt.api.model.Tier;
import org.wso2.carbon.appmgt.impl.dao.AppMDAO;
import org.wso2.carbon.appmgt.impl.service.ServiceReferenceHolder;
import org.wso2.carbon.appmgt.impl.utils.AppManagerUtil;
import org.wso2.carbon.appmgt.impl.utils.TierNameComparator;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.realm.RegistryAuthorizationManager;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The basic abstract implementation of the core APIManager interface. This implementation uses
 * the governance system registry for storing APIs and related metadata.
 */
public abstract class AbstractAPIManager implements APIManager {

    protected Log log = LogFactory.getLog(getClass());

    protected Registry registry;
    protected AppMDAO appMDAO;
    protected int tenantId;
    protected String tenantDomain;
    protected String username;
    protected AppRepository appRepository;

    public AbstractAPIManager() throws AppManagementException {
    }

    public AbstractAPIManager(String username) throws AppManagementException {
        appMDAO = new AppMDAO();
        UserRegistry configRegistry;
        try {
            if (username == null) {
                this.registry = ServiceReferenceHolder.getInstance().
                        getRegistryService().getGovernanceUserRegistry();
                configRegistry = ServiceReferenceHolder.getInstance().getRegistryService().
                        getConfigSystemRegistry();
                this.username= CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME;
            } else {
                String tenantDomainName = MultitenantUtils.getTenantDomain(username);
                String tenantUserName = MultitenantUtils.getTenantAwareUsername(username);
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getTenantId(tenantDomainName);
                this.tenantId=tenantId;
                this.tenantDomain=tenantDomainName;
                this.username=tenantUserName;

                AppManagerUtil.loadTenantRegistry(tenantId);

                this.registry = ServiceReferenceHolder.getInstance().
                        getRegistryService().getGovernanceUserRegistry(tenantUserName, tenantId);
                configRegistry = ServiceReferenceHolder.getInstance().getRegistryService().
                        getConfigSystemRegistry(tenantId);
                //load resources for each tenants.
                AppManagerUtil.loadloadTenantAPIRXT( tenantUserName, tenantId);
                AppManagerUtil.loadTenantAPIPolicy( tenantUserName, tenantId);
                AppManagerUtil.writeDefinedSequencesToTenantRegistry(tenantId);
            }
            appRepository = new DefaultAppRepository(this.registry);
            ServiceReferenceHolder.setUserRealm(ServiceReferenceHolder.getInstance().
                    getRegistryService().getConfigSystemRegistry().getUserRealm());
            registerCustomQueries(configRegistry, username);
        } catch (RegistryException e) {
            handleException("Error while obtaining registry objects", e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            handleException("Error while getting user registry for user:"+username, e);
        } catch (AppManagementException e) {
            handleException("Error while loading tenant for user:"+username, e);
        }
    }

    /**
     * method to register custom registry queries
     * @param registry  Registry instance to use
     * @throws RegistryException n error
     */
    private void registerCustomQueries(UserRegistry registry, String username)
            throws RegistryException, AppManagementException {
        String latestAPIsQueryPath = RegistryConstants.QUERIES_COLLECTION_PATH + "/latest-apis";
        String resourcesByTag = RegistryConstants.QUERIES_COLLECTION_PATH + "/resource-by-tag";
        String path = RegistryUtils.getAbsolutePath(RegistryContext.getBaseInstance(),
                                                    RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + "/repository/components/org.wso2.carbon.governance");
        if (username == null) {
            try {
                UserRealm realm = ServiceReferenceHolder.getUserRealm();
                RegistryAuthorizationManager authorizationManager = new RegistryAuthorizationManager(realm);
                authorizationManager.authorizeRole(AppMConstants.ANONYMOUS_ROLE, path, ActionConstants.GET);

            } catch (UserStoreException e) {
                handleException("Error while setting the permissions", e);
            }
        }else if (!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            int tenantId = 0;
            try {
                tenantId = ServiceReferenceHolder.getInstance().getRealmService().
                        getTenantManager().getTenantId(tenantDomain);
                AuthorizationManager authManager = ServiceReferenceHolder.getInstance().getRealmService().
                        getTenantUserRealm(tenantId).getAuthorizationManager();
                authManager.authorizeRole(AppMConstants.ANONYMOUS_ROLE, path, ActionConstants.GET);
            } catch (org.wso2.carbon.user.api.UserStoreException e) {
                handleException("Error while setting the permissions", e);
            }

        }

        if (!registry.resourceExists(latestAPIsQueryPath)) {
            //Recently added APIs
            Resource resource = registry.newResource();
//            String sql =
//                    "SELECT " +
//                    "   RR.REG_PATH_ID," +
//                    "   RR.REG_NAME " +
//                    "FROM " +
//                    "   REG_RESOURCE RR " +
//                    "WHERE " +
//                    "   RR.REG_MEDIA_TYPE = 'application/vnd.wso2-api+xml' " +
//                    "ORDER BY " +
//                    "   RR.REG_LAST_UPDATED_TIME DESC ";
            String sql =
                    "SELECT " +
                    "   RR.REG_PATH_ID AS REG_PATH_ID, " +
                    "   RR.REG_NAME AS REG_NAME " +
                    "FROM " +
                    "   REG_RESOURCE RR, " +
                    "   REG_RESOURCE_PROPERTY RRP, " +
                    "   REG_PROPERTY RP " +
                    "WHERE " +
                    "   RR.REG_MEDIA_TYPE = 'application/vnd.wso2-api+xml' " +
                    "   AND RRP.REG_VERSION = RR.REG_VERSION " +
                    "   AND RP.REG_NAME = 'STATUS' " +
                    "   AND RRP.REG_PROPERTY_ID = RP.REG_ID " +
                    "   AND (RP.REG_VALUE !='DEPRECATED' AND RP.REG_VALUE !='CREATED') " +
                    "ORDER BY " +
                    "   RR.REG_LAST_UPDATED_TIME " +
                    "DESC ";
            resource.setContent(sql);
            resource.setMediaType(RegistryConstants.SQL_QUERY_MEDIA_TYPE);
            resource.addProperty(RegistryConstants.RESULT_TYPE_PROPERTY_NAME,
                                 RegistryConstants.RESOURCES_RESULT_TYPE);
            registry.put(latestAPIsQueryPath, resource);
        }
        if(!registry.resourceExists(resourcesByTag)){
            Resource resource = registry.newResource();
            String sql =
                    "SELECT " +
                    "   '/_system/governance/repository/components/org.wso2.carbon.governance' AS MOCK_PATH, " +
                    "   R.REG_UUID AS REG_UUID " +
                    "FROM " +
                    "   REG_RESOURCE_TAG RRT, " +
                    "   REG_TAG RT, " +
                    "   REG_RESOURCE R, " +
                    "   REG_PATH RP " +
                    "WHERE " +
                    "   RT.REG_TAG_NAME = ? "+
                    "   AND R.REG_MEDIA_TYPE = ? " +
                    "   AND RP.REG_PATH_ID = R.REG_PATH_ID " +
                    "   AND RT.REG_ID = RRT.REG_TAG_ID " +
                    "   AND RRT.REG_VERSION = R.REG_VERSION ";

            resource.setContent(sql);
            resource.setMediaType(RegistryConstants.SQL_QUERY_MEDIA_TYPE);
            resource.addProperty(RegistryConstants.RESULT_TYPE_PROPERTY_NAME,
                                 RegistryConstants.RESOURCE_UUID_RESULT_TYPE);
            registry.put(resourcesByTag, resource);
        }
    }

    public void cleanup() {

    }

    public boolean isAPIAvailable(APIIdentifier identifier) throws AppManagementException {
        String path = AppMConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                      identifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                      identifier.getApiName() + RegistryConstants.PATH_SEPARATOR + identifier.getVersion();
        try {
            return registry.resourceExists(path);
        } catch (RegistryException e) {
            handleException("Failed to check availability of api :" + path, e);
            return false;
        }
    }

    public Set<String> getAPIVersions(String providerName, String apiName)
            throws AppManagementException {

        Set<String> versionSet = new HashSet<String>();
        String apiPath = AppMConstants.API_LOCATION + RegistryConstants.PATH_SEPARATOR +
                         providerName + RegistryConstants.PATH_SEPARATOR + apiName;
        try {
            Resource resource = registry.get(apiPath);
            if (resource instanceof Collection) {
                Collection collection = (Collection) resource;
                String[] versionPaths = collection.getChildren();
                if (versionPaths == null || versionPaths.length == 0) {
                    return versionSet;
                }
                for (String path : versionPaths) {
                    versionSet.add(path.substring(apiPath.length() + 1));
                }
            } else {
                throw new AppManagementException("WebApp version must be a collection " + apiName);
            }
        } catch (RegistryException e) {
            handleException("Failed to get versions for WebApp: " + apiName, e);
        }
        return versionSet;
    }

    public String addIcon(String resourcePath, Icon icon) throws AppManagementException {
        try {
            Resource thumb = registry.newResource();
            thumb.setContentStream(icon.getContent());
            thumb.setMediaType(icon.getContentType());
            registry.put(resourcePath, thumb);
            if(tenantDomain.equalsIgnoreCase(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)){
            return RegistryConstants.PATH_SEPARATOR + "registry"
                   + RegistryConstants.PATH_SEPARATOR + "resource"
                   + RegistryConstants.PATH_SEPARATOR + "_system"
                   + RegistryConstants.PATH_SEPARATOR + "governance"
                   + resourcePath;
            }
            else{
                return "/t/"+tenantDomain+ RegistryConstants.PATH_SEPARATOR + "registry"
                        + RegistryConstants.PATH_SEPARATOR + "resource"
                        + RegistryConstants.PATH_SEPARATOR + "_system"
                        + RegistryConstants.PATH_SEPARATOR + "governance"
                        + resourcePath;
            }
        } catch (RegistryException e) {
            handleException("Error while adding the icon image to the registry", e);
        }
        return null;
    }

    /**
     * Checks whether the given document already exists for the given api
     *
     * @param identifier API Identifier
     * @param docName Name of the document
     * @return true if document already exists for the given api
     * @throws AppManagementException if failed to check existence of the documentation
     */
    public boolean isDocumentationExist(APIIdentifier identifier, String docName) throws AppManagementException {
        String docPath = AppMConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                identifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                identifier.getApiName() + RegistryConstants.PATH_SEPARATOR +
                identifier.getVersion() + RegistryConstants.PATH_SEPARATOR +
                AppMConstants.DOC_DIR + RegistryConstants.PATH_SEPARATOR + docName;
        try {
            return registry.resourceExists(docPath);
        } catch (RegistryException e) {
            handleException("Failed to check existence of the document :" + docPath, e);
        }
        return false;
    }

    public String getDocumentationContent(APIIdentifier identifier, String documentationName)
            throws AppManagementException {
        String contentPath = AppManagerUtil.getAPIDocPath(identifier) +
                             AppMConstants.INLINE_DOCUMENT_CONTENT_DIR + RegistryConstants.PATH_SEPARATOR +
                             documentationName;
        String tenantDomain = MultitenantUtils.getTenantDomain(AppManagerUtil.replaceEmailDomainBack(identifier.getProviderName()));
        Registry registry;
        try {
	        /* If the WebApp provider is a tenant, load tenant registry*/
	        if (!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
	            int id = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getTenantId(tenantDomain);
	            registry = ServiceReferenceHolder.getInstance().
	                    getRegistryService().getGovernanceSystemRegistry(id);
            } else {
                if (this.tenantDomain != null && !this.tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                    registry = ServiceReferenceHolder.getInstance().
                            getRegistryService().getGovernanceUserRegistry(identifier.getProviderName(), MultitenantConstants.SUPER_TENANT_ID);
                } else {
                    registry = this.registry;
                }
            }

            if (registry.resourceExists(contentPath)) {
                Resource docContent = registry.get(contentPath);
                Object content = docContent.getContent();
                if (content != null) {
                    return new String((byte[]) docContent.getContent());
                }
            }
            /* Loading WebApp definition Content - Swagger*/
            if(documentationName != null && documentationName.equals(AppMConstants.API_DEFINITION_DOC_NAME))
            {
                String swaggerDocPath = AppMConstants.API_DOC_LOCATION + RegistryConstants.PATH_SEPARATOR +
                        identifier.getApiName() +"-"  + identifier.getVersion() + RegistryConstants.PATH_SEPARATOR + AppMConstants.API_DOC_RESOURCE_NAME;
                /* WebApp Definition content will be loaded only in WebApp Provider. Hence globally initialized
           * registry can be used here.*/
                if (this.registry.resourceExists(swaggerDocPath)) {
                    Resource docContent = registry.get(swaggerDocPath);
                    Object content = docContent.getContent();
                    if (content != null) {
                        return new String((byte[]) docContent.getContent());
                    }
                }
            }
        } catch (RegistryException e) {
            String msg = "No document content found for documentation: "
                         + documentationName + " of WebApp: "+identifier.getApiName();
            handleException(msg, e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
        	handleException("Failed to get ddocument content found for documentation: "
        				 + documentationName + " of WebApp: "+identifier.getApiName(), e);
		}
        return null;
    }

    public boolean isContextExist(String context) throws AppManagementException {
    	boolean isTenantFlowStarted = false;
        try {
            Registry systemRegistry = ServiceReferenceHolder.getInstance().
                    getRegistryService().getGovernanceSystemRegistry(tenantId);
            GovernanceUtils.loadGovernanceArtifacts((UserRegistry) systemRegistry);
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)){
        		isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
        	}
            GenericArtifactManager artifactManager = new GenericArtifactManager(systemRegistry,
                                                                                AppMConstants.API_KEY);
            GenericArtifact[] artifacts = artifactManager.getAllGenericArtifacts();
            for (GenericArtifact artifact : artifacts) {
                String artifactContext = artifact.getAttribute(AppMConstants.API_OVERVIEW_CONTEXT);
                if (artifactContext.equalsIgnoreCase(context)) {
                    return true;
                }
            }
        } catch (RegistryException e) {
            handleException("Failed to check context availability : " + context, e);
        } finally {
        	if (isTenantFlowStarted) {
        		PrivilegedCarbonContext.endTenantFlow();
        	}
        }
        return false;
    }

    public void addSubscriber(Subscriber subscriber)
            throws AppManagementException {
        appMDAO.addSubscriber(subscriber);
    }

    public void updateSubscriber(Subscriber subscriber)
            throws AppManagementException {
        appMDAO.updateSubscriber(subscriber);
    }

    public Subscriber getSubscriber(int subscriberId)
            throws AppManagementException {
        return appMDAO.getSubscriber(subscriberId);
    }

    public Icon getIcon(APIIdentifier identifier) throws AppManagementException {
        String artifactPath = AppMConstants.API_IMAGE_LOCATION + RegistryConstants.PATH_SEPARATOR +
                              identifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                              identifier.getApiName() + RegistryConstants.PATH_SEPARATOR + identifier.getVersion();

        String thumbPath = artifactPath + RegistryConstants.PATH_SEPARATOR + AppMConstants.API_ICON_IMAGE;
        try {
            if (registry.resourceExists(thumbPath)) {
                Resource res = registry.get(thumbPath);
                Icon icon = new Icon(res.getContentStream(), res.getMediaType());
                return icon;
            }
        } catch (RegistryException e) {
            handleException("Error while loading WebApp icon from the registry", e);
        }
        return null;
    }

    protected void handleException(String msg, Exception e) throws AppManagementException {
        log.error(msg, e);
        throw new AppManagementException(msg, e);
    }

    protected void handleException(String msg) throws AppManagementException {
        log.error(msg);
        throw new AppManagementException(msg);
    }

    protected final void handleResourceAlreadyExistsException(String msg) throws AppMgtResourceAlreadyExistsException {
        log.error(msg);
        throw new AppMgtResourceAlreadyExistsException(msg);
    }

    protected final void handleResourceNotFoundException(String msg) throws AppMgtResourceNotFoundException {
        log.error(msg);
        throw new AppMgtResourceNotFoundException(msg);
    }

    protected final void handleResourceAuthorizationException(String msg) throws AppMgtAuthorizationFailedException {
        log.error(msg);
        throw new AppMgtAuthorizationFailedException(msg);
    }
    public boolean isApplicationTokenExists(String accessToken) throws AppManagementException {
        return appMDAO.isAccessTokenExists(accessToken);
    }

    public boolean isApplicationTokenRevoked(String accessToken) throws AppManagementException {
        return appMDAO.isAccessTokenRevoked(accessToken);
    }


    public APIKey getAccessTokenData(String accessToken) throws AppManagementException {
        return appMDAO.getAccessTokenData(accessToken);
    }

    public Map<Integer, APIKey> searchAccessToken(String searchType, String searchTerm, String loggedInUser)
            throws AppManagementException {
        if (searchType == null) {
            return appMDAO.getAccessTokens(searchTerm);
        } else {
            if (searchType.equalsIgnoreCase("User")) {
                return appMDAO.getAccessTokensByUser(searchTerm, loggedInUser);
            } else if (searchType.equalsIgnoreCase("Before")) {
                return appMDAO.getAccessTokensByDate(searchTerm, false, loggedInUser);
            }  else if (searchType.equalsIgnoreCase("After")) {
                return appMDAO.getAccessTokensByDate(searchTerm, true, loggedInUser);
            } else {
                return appMDAO.getAccessTokens(searchTerm);
            }
        }

    }

    /**
     * Returns a list of pre-defined # {@link org.wso2.carbon.appmgt.api.model.Tier} in the system.
     *
     * @return Set<Tier>
     */
    public Set<Tier> getTiers() throws AppManagementException {


        Set<Tier> tiers = new TreeSet<Tier>(new TierNameComparator());

        Map<String, Tier> tierMap;
        if (tenantId == 0) {
            tierMap = AppManagerUtil.getTiers();
        } else {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId, true);
            tierMap = AppManagerUtil.getTiers(tenantId);
            PrivilegedCarbonContext.endTenantFlow();
        }
        tiers.addAll(tierMap.values());

        return tiers;
    }

    /**
     * Returns a list of pre-defined # {@link org.wso2.carbon.appmgt.api.model.Tier} in the system.
     *
     * @return Set<Tier>
     */
    public Set<Tier> getTiers(String tenantDomain) throws AppManagementException {
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
        Set<Tier> tiers = new TreeSet<Tier>(new TierNameComparator());

        Map<String, Tier> tierMap;
        int requestedTenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        if (requestedTenantId == 0) {
            tierMap = AppManagerUtil.getTiers();
        } else {
            tierMap = AppManagerUtil.getTiers(requestedTenantId);
        }
        tiers.addAll(tierMap.values());
        PrivilegedCarbonContext.endTenantFlow();
        return tiers;
    }

    private boolean isTenantDomainNotMatching(String tenantDomain) {
        if (this.tenantDomain != null) {
            return !(this.tenantDomain.equals(tenantDomain));
        }
        return true;
    }

}
