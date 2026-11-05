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
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.BasicCredentialsProvider
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.common.security.GuardedString
import org.identityconnectors.common.security.SecurityUtil
import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def connection = connection as HttpClient
def username = username as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def password = password as GuardedString

log.info("Entering " + operation + " Script");

// Read the user's own entry with the user's credentials instead of the configured ones
def credentialsProvider = new BasicCredentialsProvider()
credentialsProvider.setCredentials(AuthScope.ANY,
        new UsernamePasswordCredentials(username, SecurityUtil.decrypt(password)));
def authCache = new BasicAuthCache()
authCache.put(new HttpHost(configuration.serviceAddress.host, configuration.serviceAddress.port,
        configuration.serviceAddress.scheme), new BasicScheme())

def context = HttpClientContext.create()
context.setCredentialsProvider(credentialsProvider)
context.setAuthCache(authCache)
// Use an empty cookie store, so the session of the connector's own credentials is not reused
context.setCookieStore(new BasicCookieStore())

def uri = new URIBuilder(configuration.serviceAddress.resolve('users/' + URLEncoder.encode(username, 'UTF-8')))
        .addParameter('_fields', '_id')
        .build()

return connection.execute(new HttpGet(uri), { HttpResponse response ->
    switch (response.statusLine.statusCode) {
        case 200:
            return new Uid(new JsonSlurper().parse(response.entity.content, 'UTF-8')._id as String)
        case 401:
            throw new InvalidCredentialException()
        default:
            throw new ConnectorSecurityException(response.statusLine.toString())
    }
} as ResponseHandler, context)
