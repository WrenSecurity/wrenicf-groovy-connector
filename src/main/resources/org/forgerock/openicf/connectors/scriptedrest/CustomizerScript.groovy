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

package org.forgerock.openicf.connectors.scriptedrest

import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.protocol.HttpContext
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration.AuthMethod
import org.identityconnectors.common.security.GuardedString

/**
 * A customizer script defines the custom closures to interact with the default implementation and customize it.
 *
 * @author Laszlo Hordos
 */
customize {
    init { HttpClientBuilder builder ->

        //SETUP: org.apache.http
        def c = delegate as ScriptedRESTConfiguration

        def httpHost = new HttpHost(c.serviceAddress?.host, c.serviceAddress?.port, c.serviceAddress?.scheme);

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);
        // Increase max connections for httpHost to 50
        cm.setMaxPerRoute(new HttpRoute(httpHost), 50);

        builder.setConnectionManager(cm)


        // configure timeout on the entire client
        RequestConfig requestConfig = RequestConfig.custom()/*
                                                             * .
                                                             * setConnectionRequestTimeout
                                                             * ( 50).
                                                             * setConnectTimeout
                                                             * (50)
                                                             * .setSocketTimeout
                                                             * (50)
                                                             */.build();
        builder.setDefaultRequestConfig(requestConfig)



        if (c.proxyAddress != null) {
            builder.setProxy(new HttpHost(c.proxyAddress?.host, c.proxyAddress?.port, c.proxyAddress?.scheme));
        }



        switch (AuthMethod.valueOf(c.defaultAuthMethod)) {
            case AuthMethod.BASIC_PREEMPTIVE:

                // Create AuthCache instance
                def authCache = new BasicAuthCache();
                // Generate BASIC scheme object and add it to the local auth cache
                authCache.put(httpHost, new BasicScheme());
                c.propertyBag.put(HttpClientContext.AUTH_CACHE, authCache)
                // Add the auth cache to each request, so the credentials are sent without waiting for a 401.
                // A request with its own context and auth cache keeps its own.
                builder.addInterceptorFirst({ HttpRequest request, HttpContext context ->
                    if (context.getAttribute(HttpClientContext.AUTH_CACHE) == null) {
                        context.setAttribute(HttpClientContext.AUTH_CACHE, authCache)
                    }
                } as HttpRequestInterceptor)

            case AuthMethod.BASIC:
                // It's part of the http client spec to request the resource anonymously
                // first and respond to the 401 with the Authorization header.
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

                c.password.access(
                        {
                            credentialsProvider.setCredentials(new AuthScope(httpHost.getHostName(), httpHost.getPort()),
                                    new UsernamePasswordCredentials(c.username, new String(it)));
                        } as GuardedString.Accessor
                );

                builder.setDefaultCredentialsProvider(credentialsProvider);
                break;
            case AuthMethod.NONE:
                break;
            default:
                throw new IllegalArgumentException();
        }

        // Use the cookie store for all requests, it is also available to scripts in the property bag
        def cookieStore = new BasicCookieStore()
        builder.setDefaultCookieStore(cookieStore)
        c.propertyBag.put(HttpClientContext.COOKIE_STORE, cookieStore);

    }

    release {
        propertyBag.clear()
    }

    decorate { HttpClient httpClient ->
        return httpClient
    }

}