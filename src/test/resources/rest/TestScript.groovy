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
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def connection = connection as HttpClient
def log = log as Log

def request = new HttpPost(configuration.serviceAddress.resolve('users?_action=clear'))
request.setEntity(new StringEntity('null', ContentType.APPLICATION_JSON))

connection.execute(request, { HttpResponse response ->
    if (response.statusLine.statusCode == 200) {
        log.ok "Action response: "
        new JsonSlurper().parse(response.entity.content, 'UTF-8').each() { key, value ->
            log.ok "  ${key} : ${value}"
        }
    } else {
        log.ok "Unexpected failure: ${response.statusLine}"
    }
} as ResponseHandler)
