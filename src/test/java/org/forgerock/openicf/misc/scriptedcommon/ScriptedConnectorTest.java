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
 */

package org.forgerock.openicf.misc.scriptedcommon;

import java.net.URL;
import java.net.URLDecoder;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ScriptedConnectorTest {

    private ConnectorFacade facade;

    @BeforeClass
    public void beforeClass() throws Exception {
        URL authenticate =
                ScriptedConnectorTest.class.getClassLoader().getResource(
                        "AuthenticateScript.groovy");
        Assert.assertNotNull(authenticate);
        URL create =
                ScriptedConnectorTest.class.getClassLoader().getResource("CreateScript.groovy");
        Assert.assertNotNull(create);
        URL delete =
                ScriptedConnectorTest.class.getClassLoader().getResource("DeleteScript.groovy");
        Assert.assertNotNull(delete);
        URL schema =
                ScriptedConnectorTest.class.getClassLoader().getResource("SchemaScript.groovy");
        Assert.assertNotNull(schema);
        URL query = ScriptedConnectorTest.class.getClassLoader().getResource("SearchScript.groovy");
        Assert.assertNotNull(query);
        URL sync = ScriptedConnectorTest.class.getClassLoader().getResource("SyncScript.groovy");
        Assert.assertNotNull(sync);
        URL test = ScriptedConnectorTest.class.getClassLoader().getResource("TestScript.groovy");
        Assert.assertNotNull(test);
        URL runScriptOnConnector =
                ScriptedConnectorTest.class.getClassLoader().getResource(
                        "/RunScriptOnConnectorScript.groovy");
        // Assert.assertNotNull(runScriptOnConnector);
        URL update =
                ScriptedConnectorTest.class.getClassLoader().getResource("UpdateScript.groovy");
        Assert.assertNotNull(update);

        ScriptedConfiguration configuration = new ScriptedConfiguration();

        configuration.setAuthenticateScriptFileName(URLDecoder.decode(authenticate.getFile(),
                "UTF-8"));
        configuration.setCreateScriptFileName(URLDecoder.decode(create.getFile(), "UTF-8"));
        configuration.setDeleteScriptFileName(URLDecoder.decode(delete.getFile(), "UTF-8"));
        configuration.setSchemaScriptFileName(URLDecoder.decode(schema.getFile(), "UTF-8"));
        configuration.setSearchScriptFileName(URLDecoder.decode(query.getFile(), "UTF-8"));
        configuration.setSyncScriptFileName(URLDecoder.decode(sync.getFile(), "UTF-8"));
        configuration.setTestScriptFileName(URLDecoder.decode(test.getFile(), "UTF-8"));
        configuration.setUpdateScriptFileName(URLDecoder.decode(update.getFile(), "UTF-8"));

        APIConfiguration impl =
                TestHelpers.createTestConfiguration(ScriptedConnector.class, configuration);
        impl.setProducerBufferSize(0);
        impl.getResultsHandlerConfiguration().setEnableAttributesToGetSearchResultsHandler(false);
        impl.getResultsHandlerConfiguration().setEnableCaseInsensitiveFilter(false);
        impl.getResultsHandlerConfiguration().setEnableFilteredResultsHandler(false);
        impl.getResultsHandlerConfiguration().setEnableNormalizingResultsHandler(false);
        facade = ConnectorFacadeFactory.getInstance().newInstance(impl);
        facade.test();
    }

    @Test
    public void testAuthenticate() throws Exception {
        facade.authenticate(ObjectClass.ACCOUNT, "test",
                new GuardedString("Passw0rd".toCharArray()), null);
    }

    @Test(expectedExceptions = InvalidPasswordException.class)
    public void testAuthenticateFail() throws Exception {
        facade.authenticate(ObjectClass.ACCOUNT, "test",
                new GuardedString("_WRONG_".toCharArray()), null);
    }

    @Test(expectedExceptions = UnknownUidException.class)
    public void testAuthenticateUnknownFail() throws Exception {
        facade.authenticate(ObjectClass.ACCOUNT, "nobody", new GuardedString("_WRONG_"
                .toCharArray()), null);
    }

    @Test
    public void testCreate() throws Exception {
        Uid uid =
                facade.create(ObjectClass.ACCOUNT, CollectionUtil.newSet(new Name("test"),
                        AttributeBuilder.buildPassword("Passw0rd".toCharArray()), AttributeBuilder
                                .buildEnabled(false)), null);
        Assert.assertNotNull(uid);

        uid =
                facade.create(ObjectClass.GROUP, CollectionUtil.newSet(new Name("test"),
                        AttributeBuilder.build("description", "Test")), null);
        Assert.assertNotNull(uid);

    }

    @Test
    public void testDelete() throws Exception {
        facade.delete(ObjectClass.ACCOUNT, new Uid("test"), null);
    }

    @Test
    public void testSchema() throws Exception {
        Schema schema = facade.schema();
        ObjectClassInfo user = schema.findObjectClassInfo(ObjectClass.ACCOUNT_NAME);
        Assert.assertNotNull(user);
    }

    @Test
    public void testExecuteQuery() throws Exception {
        Filter filter = FilterBuilder.equalTo(AttributeBuilder.build("EQ", "equals"));
        filter = FilterBuilder.not(filter);
        filter =
                FilterBuilder.or(filter, FilterBuilder.containsAllValues(AttributeBuilder.build(
                        "CA", "containsAll")));
        filter =
                FilterBuilder.or(filter, FilterBuilder.contains(AttributeBuilder.build("CO",
                        "contains")));
        filter =
                FilterBuilder.or(filter, FilterBuilder.endsWith(AttributeBuilder.build("EW",
                        "endsWith")));
        filter =
                FilterBuilder.or(filter, FilterBuilder.greaterThan(AttributeBuilder.build("GT",
                        "greaterThan")));
        filter =
                FilterBuilder.or(filter, FilterBuilder.greaterThanOrEqualTo(AttributeBuilder.build(
                        "GE", "greaterThanOrEqualTo")));

        filter =
                FilterBuilder.or(filter, FilterBuilder.lessThan(AttributeBuilder.build("LT",
                        "lessThan")));
        filter =
                FilterBuilder.or(filter, FilterBuilder.lessThanOrEqualTo(AttributeBuilder.build(
                        "LE", "lessThanOrEqualTo")));

        filter =
                FilterBuilder.and(filter, FilterBuilder.startsWith(AttributeBuilder.build("SW",
                        "startsWith")));

        TestHelpers.searchToList(facade, ObjectClass.ACCOUNT, filter);
    }

    @Test
    public void testSync() throws Exception {
        SyncToken token = facade.getLatestSyncToken(ObjectClass.ACCOUNT);
        Assert.assertEquals(token.getValue(), 42);

        facade.sync(ObjectClass.ACCOUNT, token, new SyncResultsHandler() {
            public boolean handle(SyncDelta delta) {
                return true;
            }
        }, null);

        token = facade.getLatestSyncToken(ObjectClass.GROUP);
        Assert.assertEquals(token.getValue(), 43);

        facade.sync(ObjectClass.GROUP, token, new SyncResultsHandler() {
            public boolean handle(SyncDelta delta) {
                return true;
            }
        }, null);
    }

    @Test
    public void testTest() throws Exception {
        facade.validate();
        facade.test();
    }

    @Test
    public void testRunScriptOnConnector() throws Exception {

    }

    @Test
    public void testUpdate() throws Exception {
        Uid uid =
                facade.update(ObjectClass.ACCOUNT, new Uid("test"), CollectionUtil.newSet(
                        AttributeBuilder.build("address"), AttributeBuilder.buildEnabled(false)),
                        null);
    }
}
