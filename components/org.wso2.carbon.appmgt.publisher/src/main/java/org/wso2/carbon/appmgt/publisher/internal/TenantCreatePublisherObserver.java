/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.appmgt.publisher.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appmgt.api.AppManagementException;
import org.wso2.carbon.appmgt.impl.AppMConstants;
import org.wso2.carbon.appmgt.impl.config.ConfigurationException;
import org.wso2.carbon.appmgt.impl.config.TenantConfiguration;
import org.wso2.carbon.appmgt.impl.config.TenantConfigurationLoader;
import org.wso2.carbon.appmgt.impl.utils.AppManagerUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;

/**
 * Load configuration files to tenant's registry.
 */
public class TenantCreatePublisherObserver extends AbstractAxis2ConfigurationContextObserver {
    private static final Log log = LogFactory.getLog(TenantCreatePublisherObserver.class);
    public void createdConfigurationContext(ConfigurationContext configurationContext) {
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();

        try {
            //load workflow-extension configuration to the registry.
            AppManagerUtil.loadTenantWorkFlowExtensions(tenantId);
        } catch (AppManagementException e) {
            log.error("Failed to load workflow-extension.xml to tenant " + tenantDomain + "'s registry");
        }

        try {
            //load external-stores configuration to the registry
            AppManagerUtil.loadTenantExternalStoreConfig(tenantId);
        } catch (AppManagementException e) {
            log.error("Failed to load external-stores.xml to tenant " + tenantDomain + "'s registry");
        }

        try {
            AppManagerUtil.createTenantSpecificConfigurationFilesInRegistry(tenantId);
        } catch (AppManagementException e) {
            log.error(String.format("Failed to load oauth-scope-role-mapping and custom property definitions to tenant %s's registry", tenantDomain), e);
        }

        try{
            // Write the tenant configuration file to the tenant registry.
            AppManagerUtil.createTenantConfInRegistry(tenantId);

            // Load the tenant configuration to memory
            TenantConfiguration tenantConfiguration = new TenantConfigurationLoader().load(tenantId);
            ServiceReferenceHolder.getInstance().getTenantConfigurationService().addTenantConfiguration(tenantConfiguration);

        }catch (AppManagementException e){
            log.error(String.format("Failed to create carbon-appmgt tenant specific configuration file in the registry of the tenant '%s'", tenantDomain), e);
        } catch (ConfigurationException e) {
            log.error(String.format("Failed to load carbon-appmgt tenant specific configurations from the registry for the tenant '%s'", tenantDomain), e);
        }

        try {
            //Add the creator & publisher roles if not exists
            //Apply permissons to appmgt collection for creator role
            UserRealm realm = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserRealm();

            Permission[] creatorPermissions = new Permission[]{
                    new Permission(AppMConstants.Permissions.LOGIN, UserMgtConstants.EXECUTE_ACTION),
                    new Permission(AppMConstants.Permissions.MOBILE_APP_CREATE, UserMgtConstants.EXECUTE_ACTION),
                    new Permission(AppMConstants.Permissions.MOBILE_APP_DELETE, UserMgtConstants.EXECUTE_ACTION),
                    new Permission(AppMConstants.Permissions.MOBILE_APP_UPDATE, UserMgtConstants.EXECUTE_ACTION),
                    new Permission(AppMConstants.Permissions.IDENTITY_APPLICATION_MANAGEMENT, UserMgtConstants.EXECUTE_ACTION),
                    new Permission(AppMConstants.Permissions.IDENTITY_IDP_MANAGEMENT, UserMgtConstants.EXECUTE_ACTION)};

            AppManagerUtil.addNewRole(AppMConstants.CREATOR_ROLE, creatorPermissions, realm);

            Permission[] publisherPermissions = new Permission[]{
                    new Permission(AppMConstants.Permissions.LOGIN, UserMgtConstants.EXECUTE_ACTION),
                    new Permission(AppMConstants.Permissions.MOBILE_APP_PUBLISH, UserMgtConstants.EXECUTE_ACTION)};

            AppManagerUtil.addNewRole(AppMConstants.PUBLISHER_ROLE,publisherPermissions, realm);

            //Add the store-admin role
            Permission[] storeAdminPermissions = new Permission[]
                    {new Permission(AppMConstants.Permissions.LOGIN, UserMgtConstants.EXECUTE_ACTION)};
            AppManagerUtil.addNewRole(AppMConstants.STORE_ADMIN_ROLE, storeAdminPermissions , realm);

        } catch(AppManagementException e) {
            log.error("App manager configuration service is set to publisher bundle");
        }

        try{
            AppManagerUtil.writeDefinedSequencesToTenantRegistry(tenantId);
        }catch(AppManagementException e){
            log.error("Failed to write defined sequences to tenant " + tenantDomain + "'s registry");
        }
    }
}
