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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.common.security.SecurityUtil
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.OperationalAttributes
import org.identityconnectors.framework.common.objects.Uid

def operation = operation as OperationType
def createAttributes = attributes as Set<Attribute>
def configuration = configuration as ScriptedRESTConfiguration
def connection = connection as HttpClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

String path
switch (objectClass) {
    case ObjectClass.ACCOUNT:
        path = 'users/'
        break
    case ObjectClass.GROUP:
        path = 'groups/'
        break
    default:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
}

// Store the attributes as JSON fields, the resource ID is the name
def content = [:]
createAttributes.each { attribute ->
    if (attribute.is(OperationalAttributes.PASSWORD_NAME)) {
        content.password = SecurityUtil.decrypt(AttributeUtil.getGuardedStringValue(attribute))
    } else if (attribute.is(OperationalAttributes.ENABLE_NAME)) {
        content.enabled = AttributeUtil.getBooleanValue(attribute)
    } else if (!AttributeUtil.isSpecial(attribute)) {
        content[attribute.name] = attribute.value?.size() == 1 ? attribute.value[0] : attribute.value
    }
}

// PUT with If-None-Match: * creates the resource, or fails when it already exists
def request = new HttpPut(configuration.serviceAddress.resolve(path + URLEncoder.encode(name, 'UTF-8')))
request.setHeader('If-None-Match', '*')
request.setEntity(new StringEntity(JsonOutput.toJson(content), ContentType.APPLICATION_JSON))

return connection.execute(request, { HttpResponse response ->
    def status = response.statusLine.statusCode
    if (status == 412) {
        throw new AlreadyExistsException("Object ${name} already exists")
    }
    if (status >= 300) {
        throw new ConnectorException(response.statusLine.toString())
    }
    def json = new JsonSlurper().parse(response.entity.content, 'UTF-8')
    return new Uid(json._id as String, json._rev as String)
} as ResponseHandler)
