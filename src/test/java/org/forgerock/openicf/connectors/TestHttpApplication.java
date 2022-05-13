/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openicf.connectors;

import static org.forgerock.json.resource.Applications.simpleCrestApplication;
import static org.forgerock.json.resource.Resources.newHandler;
import static org.forgerock.json.resource.Resources.newInternalConnectionFactory;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;
import static org.forgerock.json.resource.http.CrestHttp.newHttpHandler;

import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.Router;
import org.forgerock.util.Factory;

/**
 * Http Application implementation to demonstrate integration with the Commons HTTP Framework.
 */
public class TestHttpApplication implements HttpApplication {

    @Override
    public Handler start() throws HttpApplicationException {
        Router router = new Router();
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/rest/users"), newHandler(new MemoryBackend()));
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/rest/groups"), newHandler(new MemoryBackend()));
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/crest/users"), newHandler(new MemoryBackend()));
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/crest/groups"), newHandler(new MemoryBackend()));
        return newHttpHandler(simpleCrestApplication(newInternalConnectionFactory(router), "testhttpapp", "1.0"));
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {

    }
}