/*
 * Copyright 2007 Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.performance;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class CollectionContainsUrlInspection extends BaseInspection {

    @NotNull
    public String getDisplayName() {
        return InspectionGadgetsBundle.message(
                "collection.contains.url.display.name");
    }

    @Nls @NotNull
    public String getGroupDisplayName() {
        return GroupNames.PERFORMANCE_GROUP_NAME;
    }

    @NotNull
    protected String buildErrorString(Object... infos) {
        final ClassType type = (ClassType) infos[0];
        return InspectionGadgetsBundle.message(
                "collection.contains.url.problem.decriptor", type);
    }

    public BaseInspectionVisitor buildVisitor() {
        return new CollectionContainsUrlVisitor();
    }

    private static class CollectionContainsUrlVisitor
            extends BaseInspectionVisitor {

        public void visitVariable(PsiVariable variable) {
            super.visitVariable(variable);
            final PsiTypeElement typeElement = variable.getTypeElement();
            if (typeElement == null) {
                return;
            }
            final PsiType type = typeElement.getType();
            if (!(type instanceof PsiClassType)) {
                return;
            }
            final PsiJavaCodeReferenceElement referenceElement =
                    typeElement.getInnermostComponentReferenceElement();
            if (referenceElement == null) {
                return;
            }
            final PsiClassType classType = (PsiClassType) type;
            final PsiClass aClass = classType.resolve();
            final ClassType collectionType = getClassType(aClass);
            if (collectionType == ClassType.OTHER) {
                return;
            }
            final PsiReferenceParameterList parameterList =
                    referenceElement.getParameterList();
            if (parameterList == null) {
                final PsiMember member =
                        PsiTreeUtil.getParentOfType(variable, PsiMember.class);
                if (member == null) {
                    return;
                }
                final UrlAddedVisitor visitor =
                        new UrlAddedVisitor(variable, collectionType);
                member.accept(visitor);
                if (visitor.isUrlAdded()) {
                    registerVariableError(variable, collectionType);
                }
                return;
            }
            final PsiType[] typeArguments = parameterList.getTypeArguments();
            boolean containsUrl = false;
            for (PsiType typeArgument : typeArguments) {
                if (typeArgument.equalsToText("java.net.URL")) {
                    containsUrl = true;
                    break;
                }
            }
            if (!containsUrl) {
                return;
            }
            registerVariableError(variable, collectionType);
        }

        private static ClassType getClassType(PsiClass aClass) {
            return isMapOrSet(aClass, new HashSet());
        }

        private static ClassType isMapOrSet(
                PsiClass aClass, Set<PsiClass> visitedClasses){
            if (!visitedClasses.add(aClass)){
                return ClassType.OTHER;
            }
            @NonNls final String className = aClass.getQualifiedName();
            if ("java.util.Set".equals(className)){
                return ClassType.SET;
            } else if ("java.util.Map".equals(className)){
                return ClassType.MAP;
            }
            final PsiClass[] supers = aClass.getSupers();
            for (PsiClass aSuper : supers){
                final ClassType classType =
                        isMapOrSet(aSuper, visitedClasses);
                if (classType != ClassType.OTHER){
                    return classType;
                }
            }
            return ClassType.OTHER;
        }
    }

    private static class UrlAddedVisitor
            extends PsiRecursiveElementVisitor {

        private boolean urlAdded = false;
        private final PsiVariable variable;
        private final ClassType collectionType;

        UrlAddedVisitor(PsiVariable variable,
                        ClassType collectionType) {
            this.variable = variable;
            this.collectionType = collectionType;
        }

        public void visitMethodCallExpression(
                PsiMethodCallExpression expression) {
            if (urlAdded) {
                return;
            }
            super.visitMethodCallExpression(expression);
            final PsiReferenceExpression methodExpression =
                    expression.getMethodExpression();
            final PsiExpression qualifierExpression =
                    methodExpression.getQualifierExpression();
            if (!(qualifierExpression instanceof PsiReferenceExpression)) {
                return;
            }
            final PsiReferenceExpression referenceExpression =
                    (PsiReferenceExpression) qualifierExpression;

            final String methodName = methodExpression.getReferenceName();
            if (collectionType == ClassType.SET &&
                    !"add".equals(methodName)) {
                return;
            }
            if (collectionType == ClassType.MAP &&
                    !"put".equals(methodName)) {
                return;
            }
            final PsiExpressionList argumentList =
                    expression.getArgumentList();
            final PsiExpression[] arguments = argumentList.getExpressions();
            if (arguments.length != 1) {
                return;
            }
            final PsiExpression argument = arguments[0];
            final PsiType argumentType = argument.getType();
            if (argumentType == null ||
                    !argumentType.equalsToText("java.net.URL")) {
                return;
            }
            final PsiElement element = referenceExpression.resolve();
            if (!variable.equals(element)) {
                return;
            }
            urlAdded = true;
        }

        public boolean isUrlAdded() {
            return urlAdded;
        }
    }

    enum ClassType {
        SET, MAP, OTHER
    }
}