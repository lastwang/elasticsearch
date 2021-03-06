/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.painless.node;

import org.elasticsearch.painless.AnalyzerCaster;
import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.Scope;
import org.elasticsearch.painless.Scope.Variable;
import org.elasticsearch.painless.ir.BlockNode;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.ir.ForEachSubIterableNode;
import org.elasticsearch.painless.lookup.PainlessCast;
import org.elasticsearch.painless.lookup.PainlessMethod;
import org.elasticsearch.painless.lookup.def;
import org.elasticsearch.painless.symbol.ScriptRoot;

import java.util.Iterator;
import java.util.Objects;

import static org.elasticsearch.painless.lookup.PainlessLookupUtility.typeToCanonicalTypeName;

/**
 * Represents a for-each loop for iterables.
 */
public class SSubEachIterable extends AStatement {

    protected final Variable variable;
    protected final AExpression.Output expressionOutput;
    protected final Output blockOutput;

    SSubEachIterable(Location location, Variable variable, AExpression.Output expressionOutput, Output blockOutput) {
        super(location);

        this.variable = Objects.requireNonNull(variable);
        this.expressionOutput = Objects.requireNonNull(expressionOutput);
        this.blockOutput = blockOutput;
    }

    @Override
    Output analyze(ClassNode classNode, ScriptRoot scriptRoot, Scope scope, Input input) {
        Output output = new Output();

        // We must store the iterator as a variable for securing a slot on the stack, and
        // also add the location offset to make the name unique in case of nested for each loops.
        Variable iterator = scope.defineVariable(location, Iterator.class, "#itr" + location.getOffset(), true);

        PainlessMethod method;

        if (expressionOutput.actual == def.class) {
            method = null;
        } else {
            method = scriptRoot.getPainlessLookup().lookupPainlessMethod(expressionOutput.actual, false, "iterator", 0);

            if (method == null) {
                    throw createError(new IllegalArgumentException(
                            "method [" + typeToCanonicalTypeName(expressionOutput.actual) + ", iterator/0] not found"));
            }
        }

        PainlessCast cast = AnalyzerCaster.getLegalCast(location, def.class, variable.getType(), true, true);

        ForEachSubIterableNode forEachSubIterableNode = new ForEachSubIterableNode();
        forEachSubIterableNode.setConditionNode(expressionOutput.expressionNode);
        forEachSubIterableNode.setBlockNode((BlockNode)blockOutput.statementNode);
        forEachSubIterableNode.setLocation(location);
        forEachSubIterableNode.setVariableType(variable.getType());
        forEachSubIterableNode.setVariableName(variable.getName());
        forEachSubIterableNode.setCast(cast);
        forEachSubIterableNode.setIteratorType(iterator.getType());
        forEachSubIterableNode.setIteratorName(iterator.getName());
        forEachSubIterableNode.setMethod(method);
        forEachSubIterableNode.setContinuous(false);

        output.statementNode = forEachSubIterableNode;

        return output;
    }

    @Override
    public String toString() {
        //return singleLineToString(variable.getCanonicalTypeName(), variable.getName(), expression, block);
        return null;
    }
}
