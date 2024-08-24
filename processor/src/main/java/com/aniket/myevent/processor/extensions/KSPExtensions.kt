package com.aniket.myevent.processor.extensions

import com.aniket.myevent.processor.isNotKotlinPrimitive
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterSpec

object KSPExtensions {

    fun List<KSValueParameter>.getArgumentParams(): String {
        return this.map { it.name?.getShortName() }.joinToString(",")
    }

    fun KSFunctionDeclaration.invocationWithArgumentName(): String {
        return "${packageName.asString()}${simpleName.asString()}(${parameters.getArgumentParams()})"
    }

    fun KSFunctionDeclaration.functionArguments(): List<ParameterSpec> {
        val funBuilder = mutableListOf<ParameterSpec>()
        for(param in parameters) {
            val ksType = param.type.resolve()
            val className = if(!param.isNotKotlinPrimitive()) {
                param.type.element.toString()
            }
            else {
                val returnPackageName = ksType.declaration.packageName.asString()
                val qualifiedName = ksType.declaration.qualifiedName!!.getShortName()
                "$returnPackageName.$qualifiedName"
            }
            funBuilder.add(
                ParameterSpec.builder(
                    param.name!!.getShortName(),
                    ClassName.bestGuess(className)
                ).build()
            )
        }
        return funBuilder
    }

    fun KSFunctionDeclaration.getClassReturnType(): ClassName? {
        val returnKsType = returnType?.resolve()
        val returnPackageName = returnKsType?.declaration?.packageName?.asString()?.let {
            "$it."
        } ?: ""
        returnType?.element?.let {
            val myReturnType = returnPackageName + it.toString()
            return ClassName.bestGuess(myReturnType)
        }

        return null
    }

    fun KSFunctionDeclaration.containsAnnotation(qualifierName: String?): Boolean {
        return this.annotations.firstOrNull {
            it.annotationType.resolve().declaration
                .qualifiedName?.asString() == qualifiedName?.toString()
        } != null
    }

    fun KSFunctionDeclaration.containsAnnotationShortName(shortName: String?): Boolean {
        return this.annotations.firstOrNull {
            it.annotationType.resolve().declaration
                .simpleName.getShortName() == shortName?.toString()
        } != null
    }

}