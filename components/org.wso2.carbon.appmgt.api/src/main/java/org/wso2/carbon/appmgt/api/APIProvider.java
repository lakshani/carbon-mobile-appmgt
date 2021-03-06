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
package org.wso2.carbon.appmgt.api;

import org.wso2.carbon.appmgt.api.dto.UserApplicationAPIUsage;
import org.wso2.carbon.appmgt.api.model.APIIdentifier;
import org.wso2.carbon.appmgt.api.model.APIStatus;
import org.wso2.carbon.appmgt.api.model.App;
import org.wso2.carbon.appmgt.api.model.AppDefaultVersion;
import org.wso2.carbon.appmgt.api.model.AppStore;
import org.wso2.carbon.appmgt.api.model.EntitlementPolicyGroup;
import org.wso2.carbon.appmgt.api.model.FileContent;
import org.wso2.carbon.appmgt.api.model.LifeCycleEvent;
import org.wso2.carbon.appmgt.api.model.MobileApp;
import org.wso2.carbon.appmgt.api.model.OneTimeDownloadLink;
import org.wso2.carbon.appmgt.api.model.Provider;
import org.wso2.carbon.appmgt.api.model.SSOProvider;
import org.wso2.carbon.appmgt.api.model.Subscriber;
import org.wso2.carbon.appmgt.api.model.Tier;
import org.wso2.carbon.appmgt.api.model.Usage;
import org.wso2.carbon.appmgt.api.model.entitlement.EntitlementPolicy;
import org.wso2.carbon.appmgt.api.model.entitlement.EntitlementPolicyPartial;
import org.wso2.carbon.appmgt.api.model.entitlement.EntitlementPolicyValidationResult;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * APIProvider responsible for providing helper functionality
 */
public interface APIProvider extends APIManager {
    /**
     * Returns a list of all #{@link org.wso2.carbon.apimgt.api.model.Provider} available on the system.
     *
     * @return Set<Provider>
     * @throws AppManagementException if failed to get Providers
     */
    public Set<Provider> getAllProviders() throws AppManagementException;

    /**
     * Get a list of all the consumers for all APIs
     *
     * @param providerId if of the provider
     * @return Set<Subscriber>
     * @throws AppManagementException if failed to get subscribed APIs of given provider
     */
    public Set<Subscriber> getSubscribersOfProvider(String providerId)
            throws AppManagementException;

    /**
     * get details of provider
     *
     * @param providerName name of the provider
     * @return Provider
     * @throws AppManagementException if failed to get Provider
     */
    public Provider getProvider(String providerName) throws AppManagementException;

    /**
     * Return Usage of given APIIdentifier
     *
     * @param apiIdentifier APIIdentifier
     * @return Usage
     */
    public Usage getUsageByAPI(APIIdentifier apiIdentifier);

    /**
     * Return Usage of given provider and WebApp
     *
     * @param providerId if of the provider
     * @param apiName    name of the WebApp
     * @return Usage
     */
    public Usage getAPIUsageByUsers(String providerId, String apiName);

    /**
     * Returns usage details of all APIs published by a provider
     *
     * @param providerId Provider Id
     * @return UserApplicationAPIUsages for given provider
     * @throws AppManagementException If failed to get UserApplicationAPIUsage
     */
    public UserApplicationAPIUsage[] getAllAPIUsageByProvider(String providerId)
            throws AppManagementException;

    /**
     * Shows how a given consumer uses the given WebApp.
     *
     * @param apiIdentifier APIIdentifier
     * @param consumerEmail E-mal Address of consumer
     * @return Usage
     */
    public Usage getAPIUsageBySubscriber(APIIdentifier apiIdentifier, String consumerEmail);

    /**
     * Returns full list of Subscribers of an WebApp
     *
     * @param identifier APIIdentifier
     * @return Set<Subscriber>
     * @throws AppManagementException if failed to get Subscribers
     */
    public Set<Subscriber> getSubscribersOfAPI(APIIdentifier identifier)
            throws AppManagementException;

    /**
     * this method returns the Set<APISubscriptionCount> for given provider and api
     *
     * @param identifier APIIdentifier
     * @return Set<APISubscriptionCount>
     * @throws AppManagementException if failed to get APISubscriptionCountByAPI
     */
    public long getAPISubscriptionCountByAPI(APIIdentifier identifier)
            throws AppManagementException;

    /**
     * This method returns the subscribed apps by users
     * @param fromDate From date
     * @param toDate To date
     * @return list of subscribed apps by users.
     * @throws AppManagementException
     */
    public Map<String, List> getSubscribedAPPsByUsers(String fromDate, String toDate)
            throws AppManagementException;

    public void addTier(Tier tier) throws AppManagementException;

    public void updateTier(Tier tier) throws AppManagementException;

    public void removeTier(Tier tier) throws AppManagementException;

    /**
     * Generates entitlement policies for the given app.
     *
     * @param apiIdentifier   ID of the app.
     * @param authorizedAdminCookie Authorized cookie to access IDP admin services
     * @throws AppManagementException when entitlement service implementation is unable to generate policies.
     */
    public void generateEntitlementPolicies(APIIdentifier apiIdentifier, String authorizedAdminCookie) throws
                                                                                                 AppManagementException;

    /**
     * Updates given entitlement policies.
     *
     * @param policies        Entitlement policies to be updated.
     * @param authorizedAdminCookie Authorized cookie to access IDP admin services
     * @throws AppManagementException when entitlement service implementation is unable to update policies.
     */
    void updateEntitlementPolicies(List<EntitlementPolicy> policies, String authorizedAdminCookie)
            throws AppManagementException;

    /**
     * Get entitlement policy content from policyId
     *
     * @param policyId        Entitlement policy id
     * @param authorizedAdminCookie Authorized cookie to access IDP admin services
     * @return Entitlement policy content
     */
    String getEntitlementPolicy(String policyId, String authorizedAdminCookie) throws AppManagementException;

    /**
     * Get web application id
     * @param uuid web application uuid
     * @return web application id
     */
    public int getWebAppId(String uuid) throws AppManagementException;

    /**
     * Save the entitlement policy partial
     *
     * @param policyPartialName Name of the policy partial
     * @param policyPartial     policy content
     * @param isSharedPartial   policy status
     * @param policyAuthor      author of the policy
     * @param policyPartialDescription policy description
     * @return policy id
     * @throws AppManagementException
     */
    int saveEntitlementPolicyPartial(String policyPartialName, String policyPartial, boolean isSharedPartial,
                                     String policyAuthor,String policyPartialDescription) throws AppManagementException;

    /**
     * Update the policy partial
     *
     * @param policyPartialId          policy partial id
     * @param policyPartial            policy content
     * @param author                   author of the partial
     * @param isShared                 policy status
     * @param policyPartialDescription policy description
     * @param authorizedAdminCookie          Authorized cookie to access IDP admin services
     * @return if update success return true else false
     * @throws AppManagementException
     */
    public boolean updateEntitlementPolicyPartial(int policyPartialId, String policyPartial,
                                                  String author, boolean isShared, String policyPartialDescription,
                                                  String authorizedAdminCookie) throws AppManagementException;

    /**
     *
     * Get policyPartial content
     * @param policyPartialId
     * @return entitlement policy
     * @throws AppManagementException
     */
    public EntitlementPolicyPartial getPolicyPartial(int policyPartialId) throws
                                                                          AppManagementException;

    /**
          * Get the apps which use the given policy partial
          *
          * @param policyPartialId policy partial id
          * @return list of apps
          * @throws AppManagementException
          */
    public List<APIIdentifier> getAssociatedApps(int policyPartialId) throws AppManagementException;

    /**
     * Delete entitlement policy partial
     *
     * @param policyPartialId
     * @param author          author of the partial
     * @return true if success else false
     * @throws AppManagementException
     */
    public boolean deleteEntitlementPolicyPartial(int policyPartialId, String author) throws
                                                                                     AppManagementException;
    /**
     * Get the list of shared policy partials
     *
     * @return list of shared policy partials
     * @throws AppManagementException
     */
    public List<EntitlementPolicyPartial> getSharedPolicyPartialsList() throws
                                                                        AppManagementException;

    /**
     * Validates the given entitlement policy partial.
     *
     * @param policyPartial
     * @return Result of the validation.
     * @throws AppManagementException
     */
    public EntitlementPolicyValidationResult validateEntitlementPolicyPartial(String policyPartial)
            throws AppManagementException;

    /**
     * Adds a new Mobile Application to the Store
     *
     * @param mobileApp Mobile application
     * @throws AppManagementException if failed to add MobileApp
     */
    public String createMobileApp(MobileApp mobileApp) throws AppManagementException;

    /**
     *
     * Creates a new versions using the attributes (inlcuding the new version number) of the given app.
     *
     * @param app
     * @return The UUID of the newly created version.
     * @throws AppManagementException
     */
    public String createNewVersion(App app)throws AppManagementException;

    /**
     * Updates an existing Mobile Application. This method must not be used to change Mobile App status. Implementations
     * should throw an exceptions when such attempts are made. All life cycle state changes
     * should be carried out using the changeAPIStatus method of this interface.
     *
     * @param mobileApp Mobile App
     * @throws AppManagementException if failed to update WebApp
     */
    public void updateMobileApp(MobileApp mobileApp) throws AppManagementException;

    /**
     * Change the lifecycle state of the specified WebApp
     *
     * @param api The WebApp whose status to be updated
     * @param status New status of the WebApp
     * @param userId User performing the WebApp state change
     * @param updateGatewayConfig Whether the changes should be pushed to the WebApp gateway or not
     * @throws AppManagementException on error
     */

    /**
     * Returns details of a Mobile App
     * @param uuid  uuid of the App
     * @return A MobileApp object related ro given identifier or null
     * @throws AppManagementException
     */
    public MobileApp getMobileApp(String uuid) throws AppManagementException;

    /**
     * Returns the details of all the life-cycle changes done per WebApp.
     *
     * @param apiId     id of the APIIdentifier
     * @return List of life-cycle events per given WebApp
     * @throws AppManagementException if failed to copy docs
     */
    public List<LifeCycleEvent> getLifeCycleEvents(APIIdentifier apiId)
            throws AppManagementException;

    /**
     *
     * Searches and returns the apps for the given search terms.
     *
     * @param appType
     * @param searchTerms
     * @return
     * @throws AppManagementException
     */
    public List<App> searchApps(String appType, Map<String, String> searchTerms) throws AppManagementException;


    /**
     * Update the subscription status
     *
     * @param apiId WebApp Identifier
     * @param subStatus Subscription Status
     * @param appId Application Id              *
     * @return int value with subscription id
     * @throws org.wso2.carbon.appmgt.api.AppManagementException
     *          If failed to update subscription status
     */
    public void updateSubscription(APIIdentifier apiId, String subStatus, int appId) throws
                                                                                   AppManagementException;

    /**
     * Update the Tier Permissions
     *
     * @param tierName Tier Name
     * @param permissionType Permission Type
     * @param roles Roles
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          If failed to update subscription status
     */
    public void updateTierPermissions(String tierName, String permissionType, String roles) throws
                                                                                            AppManagementException;

    /**
     * Get the list of Tier Permissions
     *
     * @return Tier Permission Set
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          If failed to update subscription status
     */
    public Set getTierPermissions() throws AppManagementException;

    /**
     * Moves subscriptions from one app (@{code fromApp}) to another app ({@code toApp})
     *
     * @param fromIdentifier subscriptions of this app
     * @param toIdentifier   will be moved into this app
     * @return number of subscriptions moved
     * @throws AppManagementException
     */
    public int moveSubscriptions(APIIdentifier fromIdentifier, APIIdentifier toIdentifier)
            throws AppManagementException;

    /**
     * Get the list of Custom InSequences.
     * @return List of available sequences
     * @throws AppManagementException
     */

    public List<String> getCustomInSequences()  throws AppManagementException;


    /**
     * Get the list of Custom OutSequences.
     * @return List of available sequences
     * @throws AppManagementException
     */

    public List<String> getCustomOutSequences()  throws AppManagementException;

    /**
     * This method returns the subscription count of apps for given period.
     *
     * @param provider         provider of app
     * @param fromDate         From date
     * @param toDate           To date
     * @param isSubscriptionOn if any subscription(self or enterprise) model is on or off
     * @return subscription count of apps
     * @throws AppManagementException
     */

    public Map<String, Long> getSubscriptionCountByAPPs(String provider, String fromDate, String toDate,
                                                        boolean isSubscriptionOn) throws AppManagementException;


    /**
     * Get Application wise policy group list
     *
     * @param appId Application Id
     * @return List of policy groups
     * @throws AppManagementException on error
     */
    public List<EntitlementPolicyGroup> getPolicyGroupListByApplication(int appId) throws
            AppManagementException;

    /**
     * Retrieves TRACKING_CODE sequences from APM_APP Table
     *@param uuid : Application UUID
     *@return TRACKING_CODE
     *@throws org.wso2.carbon.appmgt.api.AppManagementException
     */
    public String getTrackingID(String uuid)throws AppManagementException;

    /**
     * Get the external app stores for given identifier.
     * @param identifier WebApp Identifier
     * @return Set of App Store
     * @throws AppManagementException
     */
    public Set<AppStore> getExternalAppStores(APIIdentifier identifier) throws AppManagementException;

    /**
     * Get WebApp default version details.
     *
     * @param appName
     * @param providerName
     * @param appStatus
     * @return Default WebApp Version
     * @throws AppManagementException
     */
    public String getDefaultVersion(String appName, String providerName, AppDefaultVersion appStatus)
            throws AppManagementException;

    /**
     * Check if the given WebApp version is the default version.
     *
     * @param identifier
     * @return true if given app is the default version
     * @throws AppManagementException
     */
    public boolean isDefaultVersion(APIIdentifier identifier) throws AppManagementException;

    /**
     * Change the lifecycle status of a given application
     * @param appType application type
     * @param appId application type
     * @param action lifecycle action perform on the application
     * @throws AppManagementException
     */
    public void changeLifeCycleStatus(String appType, String appId, String action) throws AppManagementException;

    /**
     * Get allowed lifecycle actions to perform on a given application
     * @param appType application type
     * @param appId application type
     * @return list of allowed lifecycle actions perform on the app
     */
    public String[] getAllowedLifecycleActions(String appType, String appId) throws AppManagementException;

    /**
     * Add mobile application subscription for a given user
     * @param userId userId
     * @param appId application id
     * @return
     * @throws AppManagementException
     */
    public boolean subscribeMobileApp(String userId, String appId) throws AppManagementException;

    /**
     * Remove mobile application subscription for a given user
     * @param userId username
     * @param appId application id
     * @return
     * @throws AppManagementException
     */
    public boolean unSubscribeMobileApp(String userId, String appId) throws AppManagementException;

    /**
     * Updates the given app.
     *
     * @param app
     * @throws AppManagementException
     */
    void updateApp(App app)throws AppManagementException;

    /**
     * Remove mobile application binary file from storage
     * @param fileName
     * @throws AppManagementException
     */
    public void removeBinaryFromStorage(String fileName) throws AppManagementException;

    /**
     * Generate generated one-time download link URL
     * @param appId mobile application id that the one-time download link generated for
     * @throws AppManagementException
     */
    public String generateOneTimeDownloadLink(String appId) throws AppManagementException;

    /**
     * Retrieve one-time download link details from database
     * @param UUID UUID of the one-time download link
     * @return
     * @throws AppManagementException
     */
    public OneTimeDownloadLink getOneTimeDownloadLinkDetails(String UUID) throws AppManagementException;

    /**
     * Update one-time download link details in database
     * @param oneTimeDownloadLink OneTimeDownloadLink content
     * @throws AppManagementException
     */
    public void updateOneTimeDownloadLinkStatus(OneTimeDownloadLink oneTimeDownloadLink) throws AppManagementException;

    public String getGatewayEndpoint();

    public String getAppUUIDbyName(String appName, String appVersion, int tenantId) throws AppManagementException;

    public String uploadImage(FileContent fileContent) throws AppManagementException;

}
