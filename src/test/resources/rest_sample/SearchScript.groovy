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
import org.forgerock.openicf.misc.crest.CRESTFilterVisitor
import org.forgerock.openicf.misc.crest.VisitorParameter
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.filter.Filter

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def connection = connection as HttpClient
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def resultHandler = handler

log.info("Entering " + operation + " Script")


def queryFilter = 'true'
if (filter != null) {
    queryFilter = filter.accept(CRESTFilterVisitor.VISITOR, [
            translateName: { String name ->
                if (AttributeUtil.namesEqual(name, Uid.NAME)) {
                    return "_id"
                } else if (AttributeUtil.namesEqual(name, Name.NAME)) {
                    return "_id"
                } else if (AttributeUtil.namesEqual(name, "telephoneNumber")) {
                    return "contactInformation/telephoneNumber"
                } else if (AttributeUtil.namesEqual(name, "emailAddress")) {
                    return "contactInformation/emailAddress"
                } else if (AttributeUtil.namesEqual(name, "familyName")) {
                    return "name/familyName"
                } else if (AttributeUtil.namesEqual(name, "givenName")) {
                    return "name/givenName"
                } else if (AttributeUtil.namesEqual(name, "displayName")) {
                    return "displayName"
                } else if (AttributeUtil.namesEqual(name, "members")) {
                    return "members"
                } else {
                    throw new IllegalArgumentException("Unknown field name: ${name}");
                }
            },
            convertValue: { Attribute value ->
                if (AttributeUtil.namesEqual(value.name, "members")) {
                    return value.value
                } else {
                    return AttributeUtil.getStringValue(value)
                }
            }] as VisitorParameter).toString();
}

String path
switch (objectClass) {
    case ObjectClass.ACCOUNT:
        path = 'users'
        break
    case ObjectClass.GROUP:
        path = 'groups'
        break
    default:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
}

def uri = new URIBuilder(configuration.serviceAddress.resolve(path))
        .addParameter('_queryFilter', queryFilter)
        .build()

def json = connection.execute(new HttpGet(uri), { HttpResponse response ->
    if (response.statusLine.statusCode != 200) {
        throw new ConnectorException(response.statusLine.toString())
    }
    return new JsonSlurper().parse(response.entity.content, 'UTF-8')
} as ResponseHandler)

json.result.each() { value ->
    if (ObjectClass.ACCOUNT == objectClass) {
        resultHandler {
            uid value._id
            id value._id
            attribute 'telephoneNumber', value?.contactInformation?.telephoneNumber
            attribute 'emailAddress', value?.contactInformation?.emailAddress
            attribute 'familyName', value?.name?.familyName
            attribute 'givenName', value?.name?.givenName
            attribute 'displayName', value?.displayName
            attribute 'managerId', value?.manager?._id
            attribute 'managerDisplayName', value?.manager?.displayName
            attribute 'groups', value?.groups
            attribute 'created', value?.meta?.created
            attribute 'lastModified', value?.meta?.lastModified
        }
    } else {
        resultHandler {
            uid value._id
            id value._id
            attribute 'members', value?.members
            attribute 'displayName', value?.displayName
            attribute 'created', value?.meta?.created
            attribute 'lastModified', value?.meta?.lastModified
        }
    }
}

return new SearchResult(json.pagedResultsCookie as String,
        json.remainingPagedResults != null ? json.remainingPagedResults as int : -1)
