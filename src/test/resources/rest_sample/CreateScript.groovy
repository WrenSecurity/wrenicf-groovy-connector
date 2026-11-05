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

import groovy.json.JsonBuilder
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions

def operation = operation as OperationType
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def configuration = configuration as ScriptedRESTConfiguration
def connection = connection as HttpClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

log.info("Entering " + operation + " Script");

String path
def builder = new JsonBuilder()
switch (objectClass) {
    case ObjectClass.ACCOUNT:
        path = 'users/' + URLEncoder.encode(name, 'UTF-8')
        builder {
            _id name
            contactInformation {
                telephoneNumber(createAttributes.hasAttribute("telephoneNumber") ? createAttributes.findString("telephoneNumber") : "")
                emailAddress(createAttributes.hasAttribute("emailAddress") ? createAttributes.findString("emailAddress") : "")
            }
            delegate.name({
                familyName(createAttributes.hasAttribute("familyName") ? createAttributes.findString("familyName") : "")
                givenName(createAttributes.hasAttribute("givenName") ? createAttributes.findString("givenName") : "")
            })
            displayName(createAttributes.hasAttribute("displayName") ? createAttributes.findString("displayName") : "")
        }

        if (createAttributes.hasAttribute("password")) {
            builder.content["password"] = createAttributes.findString("password")
        }
        break

    case ObjectClass.GROUP:
        path = 'groups/' + URLEncoder.encode(name, 'UTF-8')
        builder {
            _id name
            members(createAttributes.hasAttribute("members") ? createAttributes.findList("members") : [])
        }
        break

    default:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
}

// PUT with If-None-Match: * creates the resource, or fails when it already exists
def request = new HttpPut(configuration.serviceAddress.resolve(path))
request.setHeader('If-None-Match', '*')
request.setEntity(new StringEntity(builder.toString(), ContentType.APPLICATION_JSON))

connection.execute(request, { HttpResponse response ->
    def status = response.statusLine.statusCode
    if (status == 412) {
        throw new AlreadyExistsException("Object ${name} already exists")
    }
    if (status >= 300) {
        throw new ConnectorException(response.statusLine.toString())
    }
} as ResponseHandler)

return name
