/*
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 *
 */
// Parameters:
// The connector sends the following:
// connection : handler to the connector's connection object
// configuration : handler to the connector's configuration object
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// action: a string describing the action ("SEARCH" here)
// log: a handler to the connector's Logger facility
// options: a handler to the OperationOptions Map
// query: the Query Map
//
// The Query map describes the filter used in the following way:
//
// query = [ operation: "CONTAINS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "ENDSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "STARTSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "EQUALS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = null : then we assume we fetch everything
//
// AND and OR filter just embed a left/right couple of queries.
// query = [ operation: "AND", left: query1, right: query2 ]
// query = [ operation: "OR", left: query1, right: query2 ]
//
// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

assert configuration != null
assert action != null
assert log != null
assert objectClass != null
assert options != null


log.info("Entering " + action + " Script");


if (query != null) {
    [
            [uid            : "41",
             id             : "test1",
             __ENABLED__    : true,
             __GROUPS__     : ["Group1", "Group2"],
             __DESCRIPTION__: null
            ]
    ]

} else { // null query, return all ids
    [
            [uid            : "41",
             id             : "test1",
             __ENABLED__    : true,
             __GROUPS__     : ["Group1", "Group2"],
             __DESCRIPTION__: null
            ],
            [uid            : "42",
             id             : "test2",
             __ENABLED__    : true,
             __GROUPS__     : ["Group1", "Group2"],
             __DESCRIPTION__: null
            ]
    ]
}