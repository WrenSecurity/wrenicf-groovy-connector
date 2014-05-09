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

assert configuration != null
assert action != null
assert log != null
assert objectClass != null

log.info("Entering " + action + " Script");


switch (action) {
    case "SYNC":
        assert token != null
        assert options != null
        switch (objectClass) {
            case "__ACCOUNT__":

                [
                        [
                                token      : 41,
                                operation  : "CREATE_OR_UPDATE|DELETE",
                                uid        : "41",
                                id         : "test1",
                                previousUid: "40",
                                password   : "Passw0rd",
                                attributes : [
                                        __ENABLED__: true,
                                        __GROUPS__ : ["Group1", "Group2"]
                                ]
                        ],
                        [
                                token      : 42,
                                operation  : "DELETE",
                                uid        : "42",
                                id         : "test2",
                                previousUid: "41",
                                password   : "Passw0rd",
                                attributes : [
                                        __ENABLED__    : true,
                                        __GROUPS__     : ["Group1", "Group2"],
                                        __DESCRIPTION__: null
                                ]
                        ]
                ]



                break

            case "__GROUP__":
                break
        }
        break

    case "GET_LATEST_SYNC_TOKEN":
        switch (objectClass) {
            case "__ACCOUNT__":
                return 42
                break

            case "__GROUP__":
                return 43
                break
        }
        break
}