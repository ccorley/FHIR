/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.path.function;

import static com.ibm.fhir.path.evaluator.FHIRPathEvaluator.SINGLETON_FALSE;
import static com.ibm.fhir.path.evaluator.FHIRPathEvaluator.SINGLETON_TRUE;
import static com.ibm.fhir.path.util.FHIRPathUtil.empty;
import static com.ibm.fhir.path.util.FHIRPathUtil.evaluatesToBoolean;
import static com.ibm.fhir.path.util.FHIRPathUtil.getSingleton;
import static com.ibm.fhir.path.util.FHIRPathUtil.getStringValue;
import static com.ibm.fhir.path.util.FHIRPathUtil.isElementNode;
import static com.ibm.fhir.path.util.FHIRPathUtil.isFalse;
import static com.ibm.fhir.path.util.FHIRPathUtil.isResourceNode;
import static com.ibm.fhir.path.util.FHIRPathUtil.isStringValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.fhir.model.annotation.Constraint;
import com.ibm.fhir.model.resource.StructureDefinition;
import com.ibm.fhir.model.type.code.IssueSeverity;
import com.ibm.fhir.model.type.code.IssueType;
import com.ibm.fhir.model.type.code.StructureDefinitionKind;
import com.ibm.fhir.model.util.ModelSupport;
import com.ibm.fhir.path.FHIRPathNode;
import com.ibm.fhir.path.FHIRPathType;
import com.ibm.fhir.path.evaluator.FHIRPathEvaluator;
import com.ibm.fhir.path.evaluator.FHIRPathEvaluator.EvaluationContext;
import com.ibm.fhir.path.exception.FHIRPathException;
import com.ibm.fhir.profile.ProfileSupport;
import com.ibm.fhir.registry.FHIRRegistry;

public class ConformsToFunction extends FHIRPathAbstractFunction {
    private static final Logger log = Logger.getLogger(ConformsToFunction.class.getName());

    @Override
    public String getName() {
        return "conformsTo";
    }

    @Override
    public int getMinArity() {
        return 1;
    }

    @Override
    public int getMaxArity() {
        return 1;
    }

    @Override
    public Collection<FHIRPathNode> apply(EvaluationContext evaluationContext, Collection<FHIRPathNode> context, List<Collection<FHIRPathNode>> arguments) {
        if (context.isEmpty()) {
            return empty();
        }

        if (!isResourceNode(context) && !isElementNode(context)) {
            throw new IllegalArgumentException("The 'conformsTo' function must be invoked on a Resource or Element node");
        }

        if (!isStringValue(arguments.get(0))) {
            throw new IllegalArgumentException("The argument to the 'conformsTo' function must be a string value");
        }

        FHIRPathNode node = getSingleton(context);
        FHIRPathType type = node.type();
        Class<?> modelClass = type.modelClass();
        String url = getStringValue(arguments.get(0)).string();

        if (FHIRRegistry.getInstance().hasResource(url, StructureDefinition.class)) {
            StructureDefinition structureDefinition = FHIRRegistry.getInstance().getResource(url,  StructureDefinition.class);

            if (FHIRPathType.FHIR_UNKNOWN_RESOURCE_TYPE.equals(type)) {
                if (!StructureDefinitionKind.RESOURCE.equals(structureDefinition.getKind())) {
                    // the profile (or base definition) is not applicable to type: UnknownResourceType
                    generateIssue(evaluationContext, IssueSeverity.ERROR, IssueType.INVALID, "Conformance check failed: profile (or base definition) '" + url + "' is not applicable to type: UnknownResourceType", node);
                    return SINGLETON_FALSE;
                }

                // unknown resource type conforms to any resource profile (or base resource definition)
                return SINGLETON_TRUE;
            }


            if (!ProfileSupport.isApplicable(structureDefinition, modelClass)) {
                // the profile (or base definition) is not applicable to type: modelClass
                generateIssue(evaluationContext, IssueSeverity.ERROR, IssueType.INVALID, "Conformance check failed: profile (or base definition) '" + url + "' is not applicable to type: " + ModelSupport.getTypeName(modelClass), node);
                return SINGLETON_FALSE;
            }

            if (node.isResourceNode() && node.asResourceNode().resource() == null) {
                // the node was created by the 'resolve' function and is not backed by a FHIR resource
                return SINGLETON_TRUE;
            }

            // save parent constraint reference
            Constraint parentConstraint = evaluationContext.getConstraint();

            List<Constraint> constraints = new ArrayList<>();
            if (ProfileSupport.isProfile(structureDefinition)) {
                // only generated constraints are checked (base model constraints should be checked by FHIRValidator)
                constraints.addAll(ProfileSupport.getConstraints(url, modelClass));
            }

            FHIRPathEvaluator evaluator = FHIRPathEvaluator.evaluator();
            for (Constraint constraint : constraints) {
                evaluationContext.setConstraint(constraint);
                try {
                    Collection<FHIRPathNode> result = evaluator.evaluate(evaluationContext, constraint.expression(), context);
                    if (evaluatesToBoolean(result) && isFalse(result)) {
                        // constraint validation failed
                        generateIssue(evaluationContext, IssueSeverity.ERROR, IssueType.INVARIANT, constraint.id() + ": " + constraint.description(), node);

                        // restore parent constraint reference
                        evaluationContext.setConstraint(parentConstraint);

                        return SINGLETON_FALSE;
                    }
                } catch (FHIRPathException e) {
                    log.log(Level.WARNING, "An unexpected error occurred while evaluating the following expression: " + constraint.expression(), e);
                }
                evaluationContext.unsetConstraint();
            }

            // restore parent constraint reference
            evaluationContext.setConstraint(parentConstraint);
        } else {
            generateIssue(evaluationContext, IssueSeverity.WARNING, IssueType.NOT_SUPPORTED, "Conformance check was not performed: profile (or base definition) '" + url + "' is not supported", node);
        }

        return SINGLETON_TRUE;
    }
}
