package com.aniket.myevent.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.aniket.myevent.annotations.MyTest
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate


class TestFunctionProcessor(
    options: Map<String, String>,
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(MyTest::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        val dependencies = Dependencies(aggregating = false,
            *resolver.getAllFiles().toList().toTypedArray()
        )

        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach {
                it.accept(MyTestClassVisitor(dependencies), Unit)
            }
        return unableToProcess.toList()
    }

    private inner class MyTestClassVisitor(val dependencies: Dependencies): KSVisitorVoid() {
        private val packageName = "com.aniket.myevent"

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            if(classDeclaration.isAbstract()) {
                logger.error(
                    "||Class Annotated with MyEvent should kotlin data class", classDeclaration
                )
            }

            if (classDeclaration.classKind != ClassKind.CLASS) {
                logger.error(
                    "||Class Annotated with Projections should kotlin data class", classDeclaration
                )
            }

            val className = classDeclaration.simpleName.getShortName()
            val classPackage = classDeclaration.packageName.asString() + "." + className

            val classVariableNameInCamelCase = className.replaceFirst(
                className[0],
                className[0].lowercaseChar()
            ) //need this for using in generated code

            logger.warn("package $classPackage")

            val constructorBuilder = StringBuilder()

            val properties = classDeclaration.primaryConstructor?.parameters ?: emptyList()

            val additionBuilder = properties.map { it.name?.getShortName() }.joinToString("+")

            for(property in properties) {
                val variableType = if(property.isVal) "val" else "var"
                val variableName = property.name?.getShortName()
                val variableReturnType = property.type.element
                constructorBuilder.append("$variableType $variableName: $variableReturnType,")
            }

            val toGenerateFileName = "${classDeclaration.simpleName.getShortName()}Test"
            val outputStream = codeGenerator.createNewFile(
                dependencies= dependencies,
                packageName,
                fileName = toGenerateFileName
            )

            outputStream.write(
                """package $packageName
                    |
                    |class $toGenerateFileName($constructorBuilder) {
                    |   fun addIntegers(): Int {
                    |       return $additionBuilder
                    |   }
                    |}
                """.trimMargin().toByteArray()
            )

        }
    }
}