/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * Portions Copyright 2026 Wren Security.
 */

import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.SyncToken

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def connection = connection as HttpClient
def log = log as Log
def objectClass = objectClass as ObjectClass

log.info("Entering " + operation + " Script");

// Reads the JSON resource. With allowMissing, returns null when it does not exist instead of failing.
def getJson = { URI uri, boolean allowMissing = false ->
    connection.execute(new HttpGet(uri), { HttpResponse response ->
        def status = response.statusLine.statusCode
        if (status == 404 && allowMissing) {
            return null
        }
        if (status != 200) {
            throw new ConnectorException(response.statusLine.toString())
        }
        return new JsonSlurper().parse(response.entity.content, 'UTF-8')
    } as ResponseHandler)
}

if (OperationType.GET_LATEST_SYNC_TOKEN.equals(operation)) {

    def json = getJson(new URIBuilder(configuration.serviceAddress.resolve('changelog'))
            .addParameter('_queryFilter', 'true')
            .addParameter('_pageSize', '1')
            .build())

    def lastToken = "0"
    json.result.each() { it ->
        lastToken = it._id
    }
    return new SyncToken(lastToken)

} else if (OperationType.SYNC.equals(operation)) {
    def token = token as Object
    log.info("Entering SYNC");

    String resourcePath
    String baseDN
    Map<String, Closure> attributes
    switch (objectClass) {
        case ObjectClass.ACCOUNT:
            resourcePath = 'users/'
            baseDN = 'ou=people,dc=example,dc=com'
            attributes = [
                    telephoneNumber: { it?.contactInformation?.telephoneNumber },
                    emailAddress: { it?.contactInformation?.emailAddress },
                    familyName: { it?.name?.familyName },
                    givenName: { it?.name?.givenName },
                    displayName: { it?.displayName },
                    managerId: { it?.manager?._id },
                    managerDisplayName: { it?.manager?.displayName },
                    groups: { it?.groups },
                    created: { it?.meta?.created },
                    lastModified: { it?.meta?.lastModified }
            ]
            break
        case ObjectClass.GROUP:
            resourcePath = 'groups/'
            baseDN = 'ou=groups,dc=example,dc=com'
            attributes = [
                    members: { it?.members },
                    displayName: { it?.displayName },
                    created: { it?.meta?.created },
                    lastModified: { it?.meta?.lastModified }
            ]
            break
        default:
            throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                    objectClass.objectClassValue + " is not supported.")
    }

    def changes = getJson(new URIBuilder(configuration.serviceAddress.resolve('changelog'))
            .addParameter('_queryFilter', "_id gt \"${token}\" and targetDN co \"${baseDN}\"")
            .addParameter('_fields', '_id,changeType,targetDN')
            .build())

    def lastToken = "0"
    changes.result.each() { changeLogEntry ->
        lastToken = changeLogEntry._id

        // The resource ID is the RDN value, e.g. "bjensen" in "uid=bjensen,ou=people,dc=example,dc=com"
        String resourceId = changeLogEntry.targetDN
        resourceId = resourceId.substring(resourceId.indexOf('=') + 1, resourceId.indexOf(','))

        handler({
            syncToken lastToken
            if ("add".equals(changeLogEntry.changeType)) {
                CREATE()
            } else if ("modify".equals(changeLogEntry.changeType)) {
                UPDATE()
            } else if ("delete".equals(changeLogEntry.changeType)) {
                DELETE()
                object {
                    uid resourceId
                    id resourceId
                    delegate.objectClass(objectClass)
                }
                return
            } else {
                CREATE_OR_UPDATE()
            }

            def value = getJson(configuration.serviceAddress.resolve(resourcePath + URLEncoder.encode(resourceId, 'UTF-8')), true)
            if (value == null) {
                // Deleted after the change was recorded
                DELETE()
                object {
                    uid resourceId
                    id resourceId
                    delegate.objectClass(objectClass)
                }
            } else {
                object {
                    uid value._id
                    id value._id
                    delegate.objectClass(objectClass)
                    attributes.each { name, getter ->
                        attribute name, getter(value)
                    }
                }
            }
        })
    }
    return new SyncToken(lastToken)

} else { // action not implemented
    log.error("Sync script: action '" + operation + "' is not implemented in this script");
}
