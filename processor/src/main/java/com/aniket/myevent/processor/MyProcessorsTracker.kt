package com.aniket.myevent.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.squareup.kotlinpoet.ClassName

object MyProcessorsTracker {

    val componentsQualifiedNames = mutableMapOf<String, String>()

    // hack for now
    fun getComponentBaseClass(): String {
        return componentsQualifiedNames.keys.first()
    }

    fun getKSClassDeclaration(resolver: Resolver,className: ClassName): KSClassDeclaration? {
        val classNameString = className.canonicalName
        val ksName: KSName = resolver.getKSNameFromString(classNameString)
        return resolver.getClassDeclarationByName(ksName)
    }

    // key is qualifiedName of the class
    // function is the method name of the required return type
    val qualifiedReturnType = mutableMapOf<String, MutableList<MethodReturnType>>()


    fun searchQualifiedReturnType(key: String, returnType: String): MethodReturnType? {
        return qualifiedReturnType[key]?.firstOrNull {
            it.returnType == returnType
        }
    }

}

data class MethodReturnType(
    val methodName: String,
    val returnType: String,
)