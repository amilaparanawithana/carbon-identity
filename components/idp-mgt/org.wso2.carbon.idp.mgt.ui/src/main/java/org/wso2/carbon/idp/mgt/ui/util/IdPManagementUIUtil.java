/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.idp.mgt.ui.util;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.idp.xsd.*;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.ui.CarbonUIUtil;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class IdPManagementUIUtil {

    private static Log log = LogFactory.getLog(IdPManagementUIUtil.class);

    /**
     * Validates an URI.
     *
     * @param uriString URI String
     * @return <code>true</code> if valid URI, <code>false</code> otherwise
     */
    public static boolean validateURI(String uriString) {

        if (uriString != null) {
            try {
                new URL(uriString);
            } catch (MalformedURLException e) {
                log.debug(e.getMessage(), e);
                return false;
            }
        } else {
            String errorMsg = "Invalid URL: \'NULL\'";
            log.debug(errorMsg);
            return false;
        }
        return true;
    }

    public static String getOpenIDUrl(HttpServletRequest request) {
        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
        String endpointURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("/carbon"));
        return (endpointURL + "/openid/");
    }

    public static String getSAML2SSOUrl(HttpServletRequest request) {
        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
        String endpointURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("/carbon"));
        return (endpointURL + "/samlsso/");
    }

    public static String getOAuth2AuthzEPURL(HttpServletRequest request) {
        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
        String endpointURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("/carbon"));
        return (endpointURL + "/oauth2/authorize/");
    }

    public static String getOAuth2TokenEPURL(HttpServletRequest request) {
        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
        String endpointURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("/carbon"));
        return (endpointURL + "/oauth2/token/");
    }

    public static String getPassiveSTSURL(HttpServletRequest request) {
        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
        String endpointURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("/carbon"));
        return (endpointURL + "/passivests/");
    }

    /**
     * @param request
     * @return
     * @throws Exception
     */
    public static IdentityProvider buildeFederatedIdentityProvider(HttpServletRequest request)
            throws Exception {

        IdentityProvider fedIdp = new IdentityProvider();

        if (ServletFileUpload.isMultipartContent(request)) {
            ServletRequestContext servletContext = new ServletRequestContext(request);
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            List items = upload.parseRequest(servletContext);
            Map<String, String> paramMap = new HashMap<String, String>();
            List<String> idpClaims = new ArrayList<String>();
            List<String> idpRoles = new ArrayList<String>();
            List<String> customAuthenticatorNames = new ArrayList<String>();
            List<String> proConnectorNames = new ArrayList<String>();

            Map<String, List<Property>> customAuthenticatorProperties = new HashMap<String, List<Property>>();
            Map<String, List<Property>> customProProperties = new HashMap<String, List<Property>>();

            IdentityProvider oldIdentityProvider = (IdentityProvider) request.getSession()
                    .getAttribute("identityProvider");

            if (ServletFileUpload.isMultipartContent(request)) {

                for (Object item : items) {
                    DiskFileItem diskFileItem = (DiskFileItem) item;
                    if (diskFileItem != null) {
                        byte[] value = diskFileItem.get();
                        String key = diskFileItem.getFieldName();
                        if ("certFile".equals(key)) {
                            paramMap.put(key, Base64.encode(value));
                        } else if ("google_prov_private_key".equals(key)) {
                            paramMap.put(key, Base64.encode(value));
                        } else if (key.startsWith("claimrowname_")) {
                            idpClaims.add(new String(value));
                            paramMap.put(key, new String(value));
                        } else if (key.startsWith("rolerowname_")) {
                            idpRoles.add(new String(value));
                            paramMap.put(key, new String(value));
                        } else if (key.startsWith("custom_auth_name")) {
                            customAuthenticatorNames.add(new String(value));
                        } else if (key.startsWith("custom_pro_name")) {
                            proConnectorNames.add(new String(value));
                        } else if (key.startsWith("cust_auth_prop_")) {
                            int length = "cust_auth_prop_".length();
                            String authPropString = new String(key).substring(length);
                            if (authPropString.indexOf("#") > 0) {
                                String authName = authPropString.substring(0,
                                        authPropString.indexOf("#"));
                                String propName = authPropString.substring(authPropString
                                        .indexOf("#") + 1);
                                String propVal = new String(value);
                                Property prop = new Property();
                                prop.setName(propName);
                                prop.setValue(propVal);

                                List<Property> propList = null;

                                if (customAuthenticatorProperties.get(authName) == null) {
                                    customAuthenticatorProperties.put(authName,
                                            new ArrayList<Property>());
                                }

                                propList = customAuthenticatorProperties.get(authName);
                                propList.add(prop);
                                customAuthenticatorProperties.put(authName, propList);
                            }
                        } else if (key.startsWith("cust_pro_prop_")) {
                            int length = "cust_pro_prop_".length();
                            String provPropString = new String(key).substring(length);
                            if (provPropString.indexOf("#") > 0) {
                                String proConName = provPropString.substring(0,
                                        provPropString.indexOf("#"));
                                String propName = provPropString.substring(provPropString
                                        .indexOf("#") + 1);
                                String propVal = new String(value);
                                Property prop = new Property();
                                prop.setName(propName);
                                prop.setValue(propVal);

                                List<Property> propList = null;

                                if (customProProperties.get(proConName) == null) {
                                    customProProperties.put(proConName, new ArrayList<Property>());
                                }

                                propList = customProProperties.get(proConName);
                                propList.add(prop);
                                customProProperties.put(proConName, propList);
                            }
                        } else {
                            paramMap.put(key, new String(value));
                        }

                        String updatedValue = paramMap.get(key);

                        if (updatedValue != null && updatedValue.trim().length() == 0) {
                            paramMap.put(key, null);
                        }
                    }
                }
            }

            if (oldIdentityProvider != null && oldIdentityProvider.getCertificate() != null) {
                paramMap.put("oldCertFile", oldIdentityProvider.getCertificate());
            }

            if (oldIdentityProvider != null
                    && oldIdentityProvider.getProvisioningConnectorConfigs() != null) {

                ProvisioningConnectorConfig[] provisioningConnectorConfig = oldIdentityProvider
                        .getProvisioningConnectorConfigs();
                for (ProvisioningConnectorConfig provisioningConnector : provisioningConnectorConfig) {
                    if (provisioningConnector.getName().equals("googleapps")) {
                        Property[] googleProperties = provisioningConnector
                                .getProvisioningProperties();
                        for (Property property : googleProperties) {
                            if (property.getName().equals("google_prov_private_key")) {
                                paramMap.put("old_google_prov_private_key", property.getValue());
                            }
                        }

                    }
                }

            }

            // build identity provider basic information.
            buildBasicInformation(fedIdp, paramMap);

            // build out-bound authentication configuration.
            buildOutboundAuthenticationConfiguration(fedIdp, paramMap);

            // build custom authenticator configuration.
            buildCustomAuthenticationConfiguration(fedIdp, customAuthenticatorNames,
                    customAuthenticatorProperties, paramMap);

            // build claim configuration.
            if (oldIdentityProvider != null
                    && oldIdentityProvider.getClaimConfig().getClaimMappings() != null) {
                buildClaimConfiguration(fedIdp, paramMap, idpClaims, oldIdentityProvider
                        .getClaimConfig().getClaimMappings());
            } else {
                buildClaimConfiguration(fedIdp, paramMap, idpClaims, null);
            }

            // build role configuration.
            if (oldIdentityProvider != null
                    && oldIdentityProvider.getPermissionAndRoleConfig() != null
                    && oldIdentityProvider.getPermissionAndRoleConfig().getRoleMappings() != null) {
                buildRoleConfiguration(fedIdp, paramMap, idpRoles, oldIdentityProvider
                        .getPermissionAndRoleConfig().getRoleMappings());
            } else {
                buildRoleConfiguration(fedIdp, paramMap, idpRoles, null);
            }

            // build in-bound provisioning configuration.
            buildInboundProvisioningConfiguration(fedIdp, paramMap);

            // build out-bound provisioning configuration.
            buildOutboundProvisioningConfiguration(fedIdp, paramMap);

            // build custom provisioning connectors.
            buildCustomProvisioningConfiguration(fedIdp, proConnectorNames, customProProperties,
                    paramMap);

        } else {
            throw new Exception("Invalid Content Type: Not multipart/form-data");
        }

        return fedIdp;
    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildOutboundProvisioningConfiguration(IdentityProvider fedIdp,
                                                               Map<String, String> paramMap) throws IdentityApplicationManagementException {

        // build SPML provisioning configuration.
        buildSPMLProvisioningConfiguration(fedIdp, paramMap);

        // build Google provisioning configuration.
        buildGoogleProvisioningConfiguration(fedIdp, paramMap);

        // build SCIM provisioning configuration.
        buildSCIMProvisioningConfiguration(fedIdp, paramMap);

        // build Salesforce provisioning configuration.
        buildSalesforceProvisioningConfiguration(fedIdp, paramMap);

    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildSPMLProvisioningConfiguration(IdentityProvider fedIdp,
                                                           Map<String, String> paramMap) throws IdentityApplicationManagementException {

        ProvisioningConnectorConfig proConnector = new ProvisioningConnectorConfig();
        proConnector.setName("spml");

        Property userNameProp = null;
        Property passwordProp = null;
        Property endPointProp = null;
        Property objectClass = null;

        if (paramMap.get("spmlProvEnabled") != null && "on".equals(paramMap.get("spmlProvEnabled"))) {
            proConnector.setEnabled(true);
        } else {
            proConnector.setEnabled(false);
        }

        if (paramMap.get("spmlProvDefault") != null && "on".equals(paramMap.get("spmlProvDefault"))) {
            fedIdp.setDefaultProvisioningConnectorConfig(proConnector);
        }

        if (paramMap.get("spml-username") != null) {
            userNameProp = new Property();
            userNameProp.setName("spml-username");
            userNameProp.setValue(paramMap.get("spml-username"));
        }

        if (paramMap.get("spml-password") != null) {
            passwordProp = new Property();
            passwordProp.setConfidential(true);
            passwordProp.setName("spml-password");
            passwordProp.setValue(paramMap.get("spml-password"));
        }

        if (paramMap.get("spml-ep") != null) {
            endPointProp = new Property();
            endPointProp.setName("spml-ep");
            endPointProp.setValue(paramMap.get("spml-ep"));
        }

        if (paramMap.get("spml-oc") != null) {
            objectClass = new Property();
            objectClass.setName("spml-oc");
            objectClass.setValue(paramMap.get("spml-oc"));
        }

        Property[] proProperties = new Property[]{userNameProp, passwordProp, endPointProp,
                objectClass};

        proConnector.setProvisioningProperties(proProperties);

        ProvisioningConnectorConfig[] proConnectors = fedIdp.getProvisioningConnectorConfigs();

        if (proConnector.getName() != null) {
            if (proConnectors == null || proConnectors.length == 0) {
                fedIdp.setProvisioningConnectorConfigs((new ProvisioningConnectorConfig[]{proConnector}));
            } else {
                fedIdp.setProvisioningConnectorConfigs(concatArrays(
                        new ProvisioningConnectorConfig[]{proConnector}, proConnectors));
            }
        }

    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildGoogleProvisioningConfiguration(IdentityProvider fedIdp,
                                                             Map<String, String> paramMap) throws IdentityApplicationManagementException {
        ProvisioningConnectorConfig proConnector = new ProvisioningConnectorConfig();
        proConnector.setName("googleapps");

        Property domainName = null;
        Property emailClaim = null;
        Property givenNameClaim = null;
        Property givenNameDefaultVal = null;
        Property familyNameClaim = null;
        Property familyNameDefault = null;
        Property serviceAccEmail = null;
        Property privateKey = null;
        Property adminEmail = null;
        Property appName = null;
        Property googleProvPatten = null;
        Property googleProvSeparator = null;
        String oldGooglePvtKey = null;
        String newGooglePvtKey = null;

        if (paramMap.get("googleProvEnabled") != null
                && "on".equals(paramMap.get("googleProvEnabled"))) {
            proConnector.setEnabled(true);
        } else {
            proConnector.setEnabled(false);
        }

        if (paramMap.get("googleProvDefault") != null
                && "on".equals(paramMap.get("googleProvDefault"))) {
            fedIdp.setDefaultProvisioningConnectorConfig(proConnector);
        }

        if (paramMap.get("google_prov_domain_name") != null) {
            domainName = new Property();
            domainName.setName("google_prov_domain_name");
            domainName.setValue(paramMap.get("google_prov_domain_name"));
        }

        if (paramMap.get("google_prov_email_claim_dropdown") != null) {
            emailClaim = new Property();
            emailClaim.setName("google_prov_email_claim_dropdown");
            emailClaim.setValue(paramMap.get("google_prov_email_claim_dropdown"));
        }

        if (paramMap.get("google_prov_givenname_claim_dropdown") != null) {
            givenNameClaim = new Property();
            givenNameClaim.setName("google_prov_givenname_claim_dropdown");
            givenNameClaim.setValue(paramMap.get("google_prov_givenname_claim_dropdown"));
        }

        if (paramMap.get("google_prov_givenname") != null) {
            givenNameDefaultVal = new Property();
            givenNameDefaultVal.setName("google_prov_givenname");
            givenNameDefaultVal.setValue(paramMap.get("google_prov_givenname"));
        }

        if (paramMap.get("google_prov_familyname_claim_dropdown") != null) {
            familyNameClaim = new Property();
            familyNameClaim.setName("google_prov_familyname_claim_dropdown");
            familyNameClaim.setValue(paramMap.get("google_prov_familyname_claim_dropdown"));
        }

        if (paramMap.get("google_prov_familyname") != null) {
            familyNameDefault = new Property();
            familyNameDefault.setName("google_prov_familyname");
            familyNameDefault.setValue(paramMap.get("google_prov_familyname"));
        }

        if (paramMap.get("google_prov_service_acc_email") != null) {
            serviceAccEmail = new Property();
            serviceAccEmail.setName("google_prov_service_acc_email");
            serviceAccEmail.setValue(paramMap.get("google_prov_service_acc_email"));
        }

        if (paramMap.get("old_google_prov_private_key") != null) {
            oldGooglePvtKey = paramMap.get("old_google_prov_private_key");
        }

        // get the value of the uploaded certificate.
        if (paramMap.get("google_prov_private_key") != null) {
            newGooglePvtKey = paramMap.get("google_prov_private_key");
        }

        if (newGooglePvtKey == null && oldGooglePvtKey != null) {
            newGooglePvtKey = oldGooglePvtKey;
        }

        if (newGooglePvtKey != null) {
            privateKey = new Property();
            privateKey.setName("google_prov_private_key");
            privateKey.setValue(newGooglePvtKey);
            privateKey.setType(IdentityApplicationConstants.ConfigElements.PROPERTY_TYPE_BLOB);
        }

        if (paramMap.get("google_prov_admin_email") != null) {
            adminEmail = new Property();
            adminEmail.setName("google_prov_admin_email");
            adminEmail.setValue(paramMap.get("google_prov_admin_email"));
        }

        if (paramMap.get("google_prov_application_name") != null) {
            appName = new Property();
            appName.setName("google_prov_application_name");
            appName.setValue(paramMap.get("google_prov_application_name"));
        }

        if (paramMap.get("google_prov_pattern") != null) {
            googleProvPatten = new Property();
            googleProvPatten.setName("google_prov_pattern");
            googleProvPatten.setValue(paramMap.get("google_prov_pattern"));
        }

        if (paramMap.get("google_prov_separator") != null) {
            googleProvSeparator = new Property();
            googleProvSeparator.setName("google_prov_separator");
            googleProvSeparator.setValue(paramMap.get("google_prov_separator"));
        }

        Property[] proProperties = new Property[]{appName, adminEmail, privateKey,
                serviceAccEmail, familyNameDefault, familyNameClaim, givenNameDefaultVal,
                givenNameClaim, emailClaim, domainName, googleProvPatten, googleProvSeparator};

        proConnector.setProvisioningProperties(proProperties);

        ProvisioningConnectorConfig[] proConnectors = fedIdp.getProvisioningConnectorConfigs();

        if (proConnector.getName() != null) {
            if (proConnectors == null || proConnectors.length == 0) {
                fedIdp.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{proConnector});
            } else {
                fedIdp.setProvisioningConnectorConfigs(concatArrays(
                        new ProvisioningConnectorConfig[]{proConnector}, proConnectors));
            }
        }
    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildSCIMProvisioningConfiguration(IdentityProvider fedIdp,
                                                           Map<String, String> paramMap) throws IdentityApplicationManagementException {
        ProvisioningConnectorConfig proConnector = new ProvisioningConnectorConfig();
        proConnector.setName("scim");

        Property userNameProp = null;
        Property passwordProp = null;
        Property userEpProp = null;
        Property groupEpProp = null;
        Property scimUserStoreDomain = null;

        if (paramMap.get("scimProvEnabled") != null && "on".equals(paramMap.get("scimProvEnabled"))) {
            proConnector.setEnabled(true);
        } else {
            proConnector.setEnabled(false);
        }

        if (paramMap.get("scimProvDefault") != null && "on".equals(paramMap.get("scimProvDefault"))) {
            fedIdp.setDefaultProvisioningConnectorConfig(proConnector);
        }

        if (paramMap.get("scim-username") != null) {
            userNameProp = new Property();
            userNameProp.setName("scim-username");
            userNameProp.setValue(paramMap.get("scim-username"));
        }

        if (paramMap.get("scim-password") != null) {
            passwordProp = new Property();
            passwordProp.setConfidential(true);
            passwordProp.setName("scim-password");
            passwordProp.setValue(paramMap.get("scim-password"));
        }

        if (paramMap.get("scim-user-ep") != null) {
            userEpProp = new Property();
            userEpProp.setName("scim-user-ep");
            userEpProp.setValue(paramMap.get("scim-user-ep"));
        }

        if (paramMap.get("scim-group-ep") != null) {
            groupEpProp = new Property();
            groupEpProp.setName("scim-group-ep");
            groupEpProp.setValue(paramMap.get("scim-group-ep"));
        }

        if (paramMap.get("scim-user-store-domain") != null) {
            scimUserStoreDomain = new Property();
            scimUserStoreDomain.setName("scim-user-store-domain");
            scimUserStoreDomain.setValue(paramMap.get("scim-user-store-domain"));
        }

        Property[] proProperties = new Property[]{userNameProp, passwordProp, userEpProp,
                groupEpProp, scimUserStoreDomain};

        proConnector.setProvisioningProperties(proProperties);

        ProvisioningConnectorConfig[] proConnectors = fedIdp.getProvisioningConnectorConfigs();

        if (proConnector.getName() != null) {
            if (proConnectors == null || proConnectors.length == 0) {
                fedIdp.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{proConnector});
            } else {
                fedIdp.setProvisioningConnectorConfigs(concatArrays(
                        new ProvisioningConnectorConfig[]{proConnector}, proConnectors));
            }
        }

    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildSalesforceProvisioningConfiguration(IdentityProvider fedIdp,
                                                                 Map<String, String> paramMap) throws IdentityApplicationManagementException {

        ProvisioningConnectorConfig proConnector = new ProvisioningConnectorConfig();
        proConnector.setName("salesforce");

        Property userNameProp = null;
        Property passwordProp = null;
        Property clentIdProp = null;
        Property clientSecretProp = null;
        Property apiVersionProp = null;
        Property domainNameProp = null;
        Property tokenEndpointProp = null;
        Property provisioningPattern = null;
        Property provisioningSeparator = null;
        Property provisioningDomain = null;

        if (paramMap.get("sfProvEnabled") != null && "on".equals(paramMap.get("sfProvEnabled"))) {
            proConnector.setEnabled(true);
        } else {
            proConnector.setEnabled(false);
        }

        if (paramMap.get("sfProvDefault") != null && "on".equals(paramMap.get("sfProvDefault"))) {
            fedIdp.setDefaultProvisioningConnectorConfig(proConnector);
        }

        if (paramMap.get("sf-username") != null) {
            userNameProp = new Property();
            userNameProp.setName("sf-username");
            userNameProp.setValue(paramMap.get("sf-username"));
        }

        if (paramMap.get("sf-password") != null) {
            passwordProp = new Property();
            passwordProp.setConfidential(true);
            passwordProp.setName("sf-password");
            passwordProp.setValue(paramMap.get("sf-password"));
        }

        if (paramMap.get("sf-clientid") != null) {
            clentIdProp = new Property();
            clentIdProp.setName("sf-clientid");
            clentIdProp.setValue(paramMap.get("sf-clientid"));
        }

        if (paramMap.get("sf-client-secret") != null) {
            clientSecretProp = new Property();
            clientSecretProp.setConfidential(true);
            clientSecretProp.setName("sf-client-secret");
            clientSecretProp.setValue(paramMap.get("sf-client-secret"));
        }

        if (paramMap.get("sf-clientid") != null) {
            clentIdProp = new Property();
            clentIdProp.setName("sf-clientid");
            clentIdProp.setValue(paramMap.get("sf-clientid"));
        }

        if (paramMap.get("sf-api-version") != null) {
            apiVersionProp = new Property();
            apiVersionProp.setName("sf-api-version");
            apiVersionProp.setValue(paramMap.get("sf-api-version"));
        }

        if (paramMap.get("sf-domain-name") != null) {
            domainNameProp = new Property();
            domainNameProp.setName("sf-domain-name");
            domainNameProp.setValue(paramMap.get("sf-domain-name"));
        }

        if (paramMap.get("sf-token-endpoint") != null) {
            tokenEndpointProp = new Property();
            tokenEndpointProp.setName("sf-token-endpoint");
            tokenEndpointProp.setValue(paramMap.get("sf-token-endpoint"));
        }

        if (paramMap.get("sf-prov-pattern") != null) {
            provisioningPattern = new Property();
            provisioningPattern.setName("sf-prov-pattern");
            provisioningPattern.setValue(paramMap.get("sf-prov-pattern"));
        }

        if (paramMap.get("sf-prov-separator") != null) {
            provisioningSeparator = new Property();
            provisioningSeparator.setName("sf-prov-separator");
            provisioningSeparator.setValue(paramMap.get("sf-prov-separator"));
        }

        if (paramMap.get("sf-prov-domainName") != null) {
            provisioningDomain = new Property();
            provisioningDomain.setName("sf-prov-domainName");
            provisioningDomain.setValue(paramMap.get("sf-prov-domainName"));
        }

        Property[] proProperties = new Property[]{userNameProp, passwordProp, clentIdProp,
                clientSecretProp, apiVersionProp, domainNameProp, tokenEndpointProp, provisioningPattern,
                provisioningSeparator, provisioningDomain};

        proConnector.setProvisioningProperties(proProperties);

        ProvisioningConnectorConfig[] proConnectors = fedIdp.getProvisioningConnectorConfigs();

        if (proConnector.getName() != null) {
            if (proConnectors == null || proConnectors.length == 0) {
                fedIdp.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{proConnector});
            } else {
                fedIdp.setProvisioningConnectorConfigs(concatArrays(
                        new ProvisioningConnectorConfig[]{proConnector}, proConnectors));
            }
        }
    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildClaimConfiguration(IdentityProvider fedIdp,
                                                Map<String, String> paramMap, List<String> idpClaims, ClaimMapping[] currentClaimMapping)
            throws IdentityApplicationManagementException {

        ClaimConfig claimConfiguration = new ClaimConfig();

        if (idpClaims != null && idpClaims.size() > 0) {
            List<Claim> idPClaimList = new ArrayList<Claim>();
            for (Iterator<String> iterator = idpClaims.iterator(); iterator.hasNext(); ) {
                String claimUri = iterator.next();
                Claim idpClaim = new Claim();
                idpClaim.setClaimUri(claimUri);
                idPClaimList.add(idpClaim);
            }
            claimConfiguration.setIdpClaims(idPClaimList.toArray(new Claim[idPClaimList.size()]));
        }

        claimConfiguration.setUserClaimURI(paramMap.get("user_id_claim_dropdown"));
        claimConfiguration.setRoleClaimURI(paramMap.get("role_claim_dropdown"));

        ClaimConfig claimConfigurationUpdated = claimMappingFromUI(claimConfiguration, paramMap);

        fedIdp.setClaimConfig(claimConfigurationUpdated);
    }

    private static ClaimConfig claimMappingFromUI(ClaimConfig claimConfiguration,
                                                  Map<String, String> paramMap) {
        Set<ClaimMapping> claimMappingList = new HashSet<ClaimMapping>();
        HashMap<String, String> advancedMapping = new HashMap<String, String>();

        int mappedClaimCount = 0;
        int advancedClaimCount = 0;

        if (paramMap.get("advanced_claim_id_count") != null) {
            advancedClaimCount = Integer.parseInt(paramMap.get("advanced_claim_id_count"));
        }

        for (int i = 0; i < advancedClaimCount; i++) {
            if (paramMap.get("advancnedIdpClaim_" + i) != null) {
                if (paramMap.get("advancedDefault_" + i) != null) {
                    advancedMapping.put(paramMap.get("advancnedIdpClaim_" + i),
                            paramMap.get("advancedDefault_" + i));
                } else { // if default value is not set. But still it is under advanced claim
                    // mapping
                    advancedMapping.put(paramMap.get("advancnedIdpClaim_" + i), "");
                }
            }
        }

        if (paramMap.get("claimrow_name_count") != null) {
            mappedClaimCount = Integer.parseInt(paramMap.get("claimrow_name_count"));
        }

        if (paramMap.get("choose_dialet_type_group").equals("choose_dialet_type1")) {
            claimConfiguration.setLocalClaimDialect(true);
            for (int i = 0; i < advancedClaimCount; i++) {
                String idPClaimURI = paramMap.get("advancnedIdpClaim_" + i);
                String defaultValue = paramMap.get("advancedDefault_" + i);
                ClaimMapping mapping = new ClaimMapping();
                Claim providerClaim = new Claim();
                providerClaim.setClaimUri(idPClaimURI);
                Claim localClaim = new Claim();
                localClaim.setClaimUri(idPClaimURI);
                mapping.setLocalClaim(localClaim);

                if (defaultValue != null) {
                    mapping.setDefaultValue(defaultValue);
                } else {
                    mapping.setDefaultValue("");
                }

                mapping.setRequested(true);
                claimMappingList.add(mapping);
            }

        } else if (paramMap.get("choose_dialet_type_group").equals("choose_dialet_type2")) {
            claimConfiguration.setLocalClaimDialect(false);
            for (int i = 0; i < mappedClaimCount; i++) {
                String idPClaimURI = paramMap.get("claimrowname_" + i);
                if (idPClaimURI != null) {
                    String localClaimURI = paramMap.get("claimrow_name_wso2_" + i);

                    ClaimMapping mapping = new ClaimMapping();
                    Claim providerClaim = new Claim();

                    providerClaim.setClaimUri(idPClaimURI);

                    Claim localClaim = new Claim();
                    localClaim.setClaimUri(localClaimURI);

                    mapping.setRemoteClaim(providerClaim);
                    mapping.setLocalClaim(localClaim);

                    if (advancedMapping.get(idPClaimURI) != null) {
                        if (!advancedMapping.get(idPClaimURI).equals("")) {
                            mapping.setDefaultValue(advancedMapping.get(idPClaimURI));
                        }
                        mapping.setRequested(true);
                    }
                    claimMappingList.add(mapping);

                }

            }
        }

        claimConfiguration.setClaimMappings(claimMappingList
                .toArray(new ClaimMapping[claimMappingList.size()]));

        return claimConfiguration;

    }

    private static void claimMappingFromFile(ClaimConfig claimConfiguration,
                                             String claimMappingFromFile) {
        String[] claimMappings;
        claimMappings = claimMappingFromFile.replaceAll("\\s", "").split(",");

        if (claimMappings != null && claimMappings.length > 0) {
            Set<ClaimMapping> claimMappingList = new HashSet<ClaimMapping>();
            for (int i = 0; i < claimMappings.length; i++) {
                String claimMappingString = claimMappings[i];
                if (claimMappingString != null) {
                    String[] splitClaimMapping = claimMappingString.split("-");
                    if (splitClaimMapping != null && splitClaimMapping.length == 2) {
                        String idPClaimURI = splitClaimMapping[0];
                        String localClaimURI = splitClaimMapping[1];

                        ClaimMapping mapping = new ClaimMapping();

                        Claim providerClaim = new Claim();
                        providerClaim.setClaimUri(idPClaimURI);

                        Claim localClaim = new Claim();
                        localClaim.setClaimUri(localClaimURI);

                        mapping.setRemoteClaim(providerClaim);
                        mapping.setLocalClaim(localClaim);
                        claimMappingList.add(mapping);
                    }
                }
            }

            claimConfiguration.setClaimMappings(claimMappingList
                    .toArray(new ClaimMapping[claimMappingList.size()]));
        }
    }

    /**
     * @param fedIdp
     * @param paramMap
     */
    private static void buildBasicInformation(IdentityProvider fedIdp, Map<String, String> paramMap) {

        String oldCertFile = null;
        String certFile = null;
        String deletePublicCert = null;

        // set identity provider name.
        fedIdp.setIdentityProviderName(paramMap.get("idPName"));

        // set identity provider display name.
        fedIdp.setDisplayName(paramMap.get("idpDisplayName"));

        if (paramMap.get("enable") != null && paramMap.get("enable").equals("1")) {
            fedIdp.setEnable(true);
        } else {
            fedIdp.setEnable(false);
        }

        // set identity provider description.
        fedIdp.setIdentityProviderDescription(paramMap.get("idPDescription"));

        if ("on".equals(paramMap.get("federation_hub_idp"))) {
            fedIdp.setFederationHub(true);
        } else {
            fedIdp.setFederationHub(false);
        }

        // set the home realm identifier of the identity provider.
        fedIdp.setHomeRealmId(paramMap.get("realmId"));

        // set the token end-point alias - in SAML request for OAuth.
        fedIdp.setAlias(paramMap.get("tokenEndpointAlias"));

        // get the value of the old certificate - if this is an update.
        if (paramMap.get("oldCertFile") != null) {
            oldCertFile = paramMap.get("oldCertFile");
        }

        // get the value of the uploaded certificate.
        if (paramMap.get("certFile") != null) {
            certFile = paramMap.get("certFile");
        }

        // check whether the certificate being deleted.
        if (paramMap.get("deletePublicCert") != null) {
            deletePublicCert = paramMap.get("deletePublicCert");
        }

        // if there is no new certificate and not a delete - use the old one.
        if (oldCertFile != null && certFile == null
                && (deletePublicCert == null || deletePublicCert.equals("false"))) {
            certFile = oldCertFile;
        }

        // set public certificate of the identity provider.
        fedIdp.setCertificate(certFile);
    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildOutboundAuthenticationConfiguration(IdentityProvider fedIdp,
                                                                 Map<String, String> paramMap) throws IdentityApplicationManagementException {
        // build OpenID authentication configuration.
        buildOpenIDAuthenticationConfiguration(fedIdp, paramMap);

        // build Facebook authentication configuration.
        buildFacebookAuthenticationConfiguration(fedIdp, paramMap);

        // build OpenID Connect authentication configuration.
        buildOpenIDConnectAuthenticationConfiguration(fedIdp, paramMap);

        // build SAML authentication configuration.
        buildSAMLAuthenticationConfiguration(fedIdp, paramMap);

        // build passive STS authentication configuration.
        buildPassiveSTSAuthenticationConfiguration(fedIdp, paramMap);

    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildOpenIDAuthenticationConfiguration(IdentityProvider fedIdp,
                                                               Map<String, String> paramMap) throws IdentityApplicationManagementException {

        FederatedAuthenticatorConfig openIdAuthnConfig = new FederatedAuthenticatorConfig();
        openIdAuthnConfig.setName("OpenIDAuthenticator");
        openIdAuthnConfig.setDisplayName("openid");

        if ("on".equals(paramMap.get("openIdEnabled"))) {
            openIdAuthnConfig.setEnabled(true);
        }

        if ("on".equals(paramMap.get("openIdDefault"))) {
            fedIdp.setDefaultAuthenticatorConfig(openIdAuthnConfig);
        }

        Property[] properties = new Property[4];

        Property property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OpenID.OPEN_ID_URL);
        property.setValue(paramMap.get("openIdUrl"));
        properties[0] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OpenID.REALM_ID);
        property.setValue(paramMap.get("realmId"));
        properties[1] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OpenID.IS_USER_ID_IN_CLAIMS);
        if ("1".equals(paramMap.get("open_id_user_id_location"))) {
            property.setValue("true");
        } else {
            property.setValue("false");
        }
        properties[2] = property;

        property = new Property();
        property.setName("commonAuthQueryParams");

        if (paramMap.get("openidQueryParam") != null
                && paramMap.get("openidQueryParam").trim().length() > 0) {
            property.setValue(paramMap.get("openidQueryParam"));
        }

        properties[3] = property;

        openIdAuthnConfig.setProperties(properties);

        FederatedAuthenticatorConfig[] authenticators = fedIdp.getFederatedAuthenticatorConfigs();
        if (paramMap.get("openIdUrl") != null && !"".equals(paramMap.get("openIdUrl"))) {
            // openIdUrl is mandatory for out-bound openid configuration.
            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{openIdAuthnConfig});
            } else {
                fedIdp.setFederatedAuthenticatorConfigs(concatArrays(
                        new FederatedAuthenticatorConfig[]{openIdAuthnConfig}, authenticators));
            }
        }
    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildFacebookAuthenticationConfiguration(IdentityProvider fedIdp,
                                                                 Map<String, String> paramMap) throws IdentityApplicationManagementException {

        FederatedAuthenticatorConfig facebookAuthnConfig = new FederatedAuthenticatorConfig();
        facebookAuthnConfig.setName("FacebookAuthenticator");
        facebookAuthnConfig.setDisplayName("facebook");

        if ("on".equals(paramMap.get("fbAuthEnabled"))) {
            facebookAuthnConfig.setEnabled(true);
        }

        if ("on".equals(paramMap.get("fbAuthDefault"))) {
            fedIdp.setDefaultAuthenticatorConfig(facebookAuthnConfig);
        }

        Property[] properties = new Property[4];
        Property property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.Facebook.CLIENT_ID);
        property.setValue(paramMap.get("fbClientId"));
        properties[0] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.Facebook.CLIENT_SECRET);
        property.setValue(paramMap.get("fbClientSecret"));
        property.setConfidential(true);
        properties[1] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.Facebook.SCOPE);
        property.setValue(paramMap.get("fbScope"));
        properties[2] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.Facebook.USER_INFO_FIELDS);
        String fbUserInfoFields = paramMap.get("fbUserInfoFields");
        if(fbUserInfoFields != null && fbUserInfoFields.endsWith(",")) {
            fbUserInfoFields = fbUserInfoFields.substring(0, fbUserInfoFields.length() - 1);
        }
        property.setValue(fbUserInfoFields);
        properties[3] = property;

        facebookAuthnConfig.setProperties(properties);

        FederatedAuthenticatorConfig[] authenticators = fedIdp.getFederatedAuthenticatorConfigs();

        if (paramMap.get("fbClientId") != null && !"".equals(paramMap.get("fbClientId"))
                && paramMap.get("fbClientSecret") != null
                && !"".equals(paramMap.get("fbClientSecret"))) {
            // facebook authenticator cannot exist without client id and client secret.
            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{facebookAuthnConfig});
            } else {
                fedIdp.setFederatedAuthenticatorConfigs(concatArrays(
                        new FederatedAuthenticatorConfig[]{facebookAuthnConfig}, authenticators));
            }
        }
    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildOpenIDConnectAuthenticationConfiguration(IdentityProvider fedIdp,
                                                                      Map<String, String> paramMap) throws IdentityApplicationManagementException {

        FederatedAuthenticatorConfig oidcAuthnConfig = new FederatedAuthenticatorConfig();
        oidcAuthnConfig.setName("OpenIDConnectAuthenticator");
        oidcAuthnConfig.setDisplayName("openidconnect");

        if ("on".equals(paramMap.get("oidcEnabled"))) {
            oidcAuthnConfig.setEnabled(true);
        }

        if ("on".equals(paramMap.get("oidcDefault"))) {
            fedIdp.setDefaultAuthenticatorConfig(oidcAuthnConfig);
        }

        Property[] properties = new Property[6];
        Property property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.Facebook.CLIENT_ID);
        property.setValue(paramMap.get("clientId"));
        properties[0] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_AUTHZ_URL);
        property.setValue(paramMap.get("authzUrl"));
        properties[1] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_TOKEN_URL);
        property.setValue(paramMap.get("tokenUrl"));
        properties[2] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.CLIENT_SECRET);
        property.setValue(paramMap.get("clientSecret"));
        property.setConfidential(true);
        properties[3] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.IS_USER_ID_IN_CLAIMS);
        properties[4] = property;
        if ("1".equals(paramMap.get("oidc_user_id_location"))) {
            property.setValue("true");
            ;
        } else {
            property.setValue("false");
        }

        property = new Property();
        property.setName("commonAuthQueryParams");

        if (paramMap.get("oidcQueryParam") != null
                && paramMap.get("oidcQueryParam").trim().length() > 0) {
            property.setValue(paramMap.get("oidcQueryParam"));
        }
        properties[5] = property;

        oidcAuthnConfig.setProperties(properties);
        FederatedAuthenticatorConfig[] authenticators = fedIdp.getFederatedAuthenticatorConfigs();

        if (paramMap.get("authzUrl") != null && !"".equals(paramMap.get("authzUrl"))
                && paramMap.get("tokenUrl") != null && !"".equals(paramMap.get("tokenUrl"))
                && paramMap.get("clientId") != null && !"".equals(paramMap.get("clientId"))
                && paramMap.get("clientSecret") != null && !"".equals(paramMap.get("clientSecret"))) {
            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{oidcAuthnConfig});
            } else {
                fedIdp.setFederatedAuthenticatorConfigs(concatArrays(
                        new FederatedAuthenticatorConfig[]{oidcAuthnConfig}, authenticators));
            }
        }
    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildPassiveSTSAuthenticationConfiguration(IdentityProvider fedIdp,
                                                                   Map<String, String> paramMap) throws IdentityApplicationManagementException {

        FederatedAuthenticatorConfig passiveSTSAuthnConfig = new FederatedAuthenticatorConfig();
        passiveSTSAuthnConfig.setName("PassiveSTSAuthenticator");
        passiveSTSAuthnConfig.setDisplayName("passivests");

        if ("on".equals(paramMap.get("passiveSTSEnabled"))) {
            passiveSTSAuthnConfig.setEnabled(true);
        }

        if ("on".equals(paramMap.get("passiveSTSDefault"))) {
            fedIdp.setDefaultAuthenticatorConfig(passiveSTSAuthnConfig);
        }

        Property[] properties = new Property[4];
        Property property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.PassiveSTS.REALM_ID);
        property.setValue(paramMap.get("passiveSTSRealm"));
        properties[0] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.PassiveSTS.PASSIVE_STS_URL);
        property.setValue(paramMap.get("passiveSTSUrl"));
        properties[1] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.IS_USER_ID_IN_CLAIMS);
        properties[2] = property;
        if ("1".equals(paramMap.get("passive_sts_user_id_location"))) {
            property.setValue("true");
            ;
        } else {
            property.setValue("false");
        }

        property = new Property();
        property.setName("commonAuthQueryParams");

        if (paramMap.get("passiveSTSQueryParam") != null
                && paramMap.get("passiveSTSQueryParam").trim().length() > 0) {
            property.setValue(paramMap.get("passiveSTSQueryParam"));
        }
        properties[3] = property;

        passiveSTSAuthnConfig.setProperties(properties);

        FederatedAuthenticatorConfig[] authenticators = fedIdp.getFederatedAuthenticatorConfigs();

        if (paramMap.get("passiveSTSUrl") != null && !"".equals(paramMap.get("passiveSTSUrl"))) {
            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{passiveSTSAuthnConfig});
            } else {
                fedIdp.setFederatedAuthenticatorConfigs(concatArrays(
                        new FederatedAuthenticatorConfig[]{passiveSTSAuthnConfig},
                        authenticators));
            }
        }

    }

    private static void buildCustomProvisioningConfiguration(IdentityProvider fedIdp,
                                                             List<String> proConnectorNames, Map<String, List<Property>> customProProperties,
                                                             Map<String, String> paramMap) throws IdentityApplicationManagementException {

        if (proConnectorNames != null && proConnectorNames.size() > 0) {

            ProvisioningConnectorConfig[] proConfigConnList = new ProvisioningConnectorConfig[proConnectorNames
                    .size()];
            int j = 0;
            for (String conName : proConnectorNames) {
                ProvisioningConnectorConfig customConfig = new ProvisioningConnectorConfig();
                customConfig.setName(conName);

                if ("on".equals(paramMap.get(conName + "_PEnabled"))) {
                    customConfig.setEnabled(true);
                }

                if ("on".equals(paramMap.get(conName + "_Default"))) {
                    fedIdp.setDefaultProvisioningConnectorConfig(customConfig);
                }

                List<Property> customProps = customProProperties.get(conName);

                if (customProps != null && customProps.size() > 0) {
                    customConfig.setProvisioningProperties(customProps
                            .toArray(new Property[customProps.size()]));
                }

                proConfigConnList[j++] = customConfig;
            }

            ProvisioningConnectorConfig[] provConnectors = fedIdp.getProvisioningConnectorConfigs();

            if (provConnectors == null || provConnectors.length == 0) {
                fedIdp.setProvisioningConnectorConfigs(proConfigConnList);
            } else {
                fedIdp.setProvisioningConnectorConfigs(concatArrays(proConfigConnList,
                        provConnectors));
            }
        }

    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildCustomAuthenticationConfiguration(IdentityProvider fedIdp,
                                                               List<String> authenticatorNames,
                                                               Map<String, List<Property>> customAuthenticatorProperties, Map<String, String> paramMap)
            throws IdentityApplicationManagementException {

        if (authenticatorNames != null && authenticatorNames.size() > 0) {

            FederatedAuthenticatorConfig[] fedAuthConfigList = new FederatedAuthenticatorConfig[authenticatorNames
                    .size()];
            int j = 0;
            for (String authName : authenticatorNames) {
                FederatedAuthenticatorConfig customConfig = new FederatedAuthenticatorConfig();
                customConfig.setName(authName);

                if ("on".equals(paramMap.get(authName + "_Enabled"))) {
                    customConfig.setEnabled(true);
                }

                if ("on".equals(paramMap.get(authName + "_Default"))) {
                    fedIdp.setDefaultAuthenticatorConfig(customConfig);
                }

                customConfig.setDisplayName(paramMap.get(authName + "_DisplayName"));

                List<Property> customProps = customAuthenticatorProperties.get(authName);

                if (customProps != null && customProps.size() > 0) {
                    customConfig
                            .setProperties(customProps.toArray(new Property[customProps.size()]));
                }

                fedAuthConfigList[j++] = customConfig;
            }

            FederatedAuthenticatorConfig[] authenticators = fedIdp
                    .getFederatedAuthenticatorConfigs();

            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticatorConfigs(fedAuthConfigList);
            } else {
                fedIdp.setFederatedAuthenticatorConfigs(concatArrays(fedAuthConfigList,
                        authenticators));
            }
        }

    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildSAMLAuthenticationConfiguration(IdentityProvider fedIdp,
                                                             Map<String, String> paramMap) throws IdentityApplicationManagementException {

        FederatedAuthenticatorConfig saml2SSOAuthnConfig = new FederatedAuthenticatorConfig();
        saml2SSOAuthnConfig.setName("SAMLSSOAuthenticator");
        saml2SSOAuthnConfig.setDisplayName("samlsso");

        if ("on".equals(paramMap.get("saml2SSOEnabled"))) {
            saml2SSOAuthnConfig.setEnabled(true);
        }

        if ("on".equals(paramMap.get("saml2SSODefault"))) {
            fedIdp.setDefaultAuthenticatorConfig(saml2SSOAuthnConfig);
        }

        Property[] properties = new Property[13];
        Property property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID);
        property.setValue(paramMap.get("idPEntityId"));
        properties[0] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.SP_ENTITY_ID);
        property.setValue(paramMap.get("spEntityId"));
        properties[1] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.SSO_URL);
        property.setValue(paramMap.get("ssoUrl"));
        properties[2] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_AUTHN_REQ_SIGNED);
        if ("on".equals(paramMap.get("authnRequestSigned"))) {
            property.setValue("true");
        } else {
            property.setValue("false");
        }
        properties[3] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_LOGOUT_ENABLED);
        if ("on".equals(paramMap.get("sloEnabled"))) {
            property.setValue("true");
        } else {
            property.setValue("false");
        }
        properties[4] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.LOGOUT_REQ_URL);
        property.setValue(paramMap.get("logoutUrl"));
        properties[5] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_LOGOUT_REQ_SIGNED);
        if ("on".equals(paramMap.get("logoutRequestSigned"))) {
            property.setValue("true");
        } else {
            property.setValue("false");
        }
        properties[6] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_AUTHN_RESP_SIGNED);
        if ("on".equals(paramMap.get("authnResponseSigned"))) {
            property.setValue("true");
        } else {
            property.setValue("false");
        }
        properties[7] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_USER_ID_IN_CLAIMS);
        if ("1".equals(paramMap.get("saml2_sso_user_id_location"))) {
            property.setValue("true");
        } else {
            property.setValue("false");
        }
        properties[8] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_ENABLE_ASSERTION_ENCRYPTION);
        if ("on".equals(paramMap.get("IsEnableAssetionEncription"))) {
            property.setValue("true");
        } else {
            property.setValue("false");
        }
        properties[9] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_ENABLE_ASSERTION_SIGNING);
        if ("on".equals(paramMap.get("isEnableAssertionSigning"))) {
            property.setValue("true");
        } else {
            property.setValue("false");
        }
        properties[10] = property;

        property = new Property();
        property.setName("commonAuthQueryParams");

        if (paramMap.get("samlQueryParam") != null
                && paramMap.get("samlQueryParam").trim().length() > 0) {
            property.setValue(paramMap.get("samlQueryParam"));
        }

        properties[11] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.REQUEST_METHOD);
        property.setValue(paramMap
                .get(IdentityApplicationConstants.Authenticator.SAML2SSO.REQUEST_METHOD));
        properties[12] = property;

        saml2SSOAuthnConfig.setProperties(properties);

        FederatedAuthenticatorConfig[] authenticators = fedIdp.getFederatedAuthenticatorConfigs();

        if (paramMap.get("ssoUrl") != null && !"".equals(paramMap.get("ssoUrl"))
                && paramMap.get("idPEntityId") != null && !"".equals(paramMap.get("idPEntityId"))) {
            if (authenticators == null || authenticators.length == 0) {
                fedIdp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{saml2SSOAuthnConfig});
            } else {
                fedIdp.setFederatedAuthenticatorConfigs(concatArrays(
                        new FederatedAuthenticatorConfig[]{saml2SSOAuthnConfig}, authenticators));
            }
        }
    }

    /**
     * @param fedIdp
     * @param paramMap
     * @param idpRoles
     * @param currentRoleMapping
     * @throws IdentityApplicationManagementException
     */

    private static void buildRoleConfiguration(IdentityProvider fedIdp,
                                               Map<String, String> paramMap, List<String> idpRoles, RoleMapping[] currentRoleMapping)
            throws IdentityApplicationManagementException {

        PermissionsAndRoleConfig roleConfiguration = new PermissionsAndRoleConfig();

        roleConfiguration.setIdpRoles(idpRoles.toArray(new String[idpRoles.size()]));

        Set<RoleMapping> roleMappingList = new HashSet<RoleMapping>();
        String idpProvisioningRole = paramMap.get("idpProvisioningRole");
        fedIdp.setProvisioningRole(idpProvisioningRole);

        int attributesCount = 0;

        if (paramMap.get("rolemappingrow_name_count") != null) {
            attributesCount = Integer.parseInt(paramMap.get("rolemappingrow_name_count"));
        }

        for (int i = 0; i < attributesCount; i++) {
            String idPRoleName = paramMap.get("rolerowname_" + i);
            String localRoleString = paramMap.get("localrowname_" + i);
            if (idPRoleName != null && localRoleString != null) {
                String[] splitLocalRole = localRoleString.split("/");
                String userStoreId = null;
                String localRoleName = null;
                LocalRole localRole = null;
                if (splitLocalRole != null && splitLocalRole.length == 2) {
                    userStoreId = splitLocalRole[0];
                    localRoleName = splitLocalRole[1];
                    localRole = new LocalRole();
                    localRole.setUserStoreId(userStoreId);
                    localRole.setLocalRoleName(localRoleName);
                } else {
                    localRoleName = localRoleString;
                    localRole = new LocalRole();
                    localRole.setLocalRoleName(localRoleName);
                }

                RoleMapping roleMapping = new RoleMapping();
                roleMapping.setLocalRole(localRole);
                roleMapping.setRemoteRole(idPRoleName);

                roleMappingList.add(roleMapping);
            }
        }

        roleConfiguration.setRoleMappings(roleMappingList.toArray(new RoleMapping[roleMappingList
                .size()]));

        fedIdp.setPermissionAndRoleConfig(roleConfiguration);

    }

    /**
     * @param fedIdp
     * @param paramMap
     * @throws IdentityApplicationManagementException
     */
    private static void buildInboundProvisioningConfiguration(IdentityProvider fedIdp,
                                                              Map<String, String> paramMap) throws IdentityApplicationManagementException {

        String provisioning = paramMap.get("provisioning");
        JustInTimeProvisioningConfig jitProvisioningConfiguration = new JustInTimeProvisioningConfig();

        if ("provision_disabled".equals(provisioning)) {
            jitProvisioningConfiguration.setProvisioningEnabled(false);
        } else if ("provision_static".equals(provisioning)
                || "provision_dynamic".equals(provisioning)) {
            jitProvisioningConfiguration.setProvisioningEnabled(true);
        }

        jitProvisioningConfiguration.setProvisioningUserStore(paramMap
                .get("provision_static_dropdown"));

        if (paramMap.get("provision_dynamic_dropdown") != null) {
            if (!"--- Select Claim URI ---".equals(paramMap.get("provision_dynamic_dropdown"))) {
                jitProvisioningConfiguration.setProvisioningUserStore(paramMap
                        .get("provision_dynamic_dropdown"));
            }
        }

        fedIdp.setJustInTimeProvisioningConfig(jitProvisioningConfiguration);

    }

    /**
     * @param o1
     * @param o2
     * @return
     */
    private static ProvisioningConnectorConfig[] concatArrays(ProvisioningConnectorConfig[] o1,
                                                              ProvisioningConnectorConfig[] o2) {
        ProvisioningConnectorConfig[] ret = new ProvisioningConnectorConfig[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

    /**
     * @param o1
     * @param o2
     * @return
     */
    private static FederatedAuthenticatorConfig[] concatArrays(FederatedAuthenticatorConfig[] o1,
                                                               FederatedAuthenticatorConfig[] o2) {
        FederatedAuthenticatorConfig[] ret = new FederatedAuthenticatorConfig[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

    public static org.wso2.carbon.identity.application.common.model.idp.xsd.FederatedAuthenticatorConfig getFederatedAuthenticator(
            org.wso2.carbon.identity.application.common.model.idp.xsd.FederatedAuthenticatorConfig[] federatedAuthenticators,
            String authenticatorName) {

        for (FederatedAuthenticatorConfig authenticator : federatedAuthenticators) {
            if (authenticator.getName().equals(authenticatorName)) {
                return authenticator;
            }
        }
        return null;
    }

    public static org.wso2.carbon.identity.application.common.model.idp.xsd.Property getProperty(
            org.wso2.carbon.identity.application.common.model.idp.xsd.Property[] properties,
            String propertyName) {

        for (org.wso2.carbon.identity.application.common.model.idp.xsd.Property property : properties) {
            if (property.getName().equals(propertyName)) {
                return property;
            }
        }
        return null;
    }
}
