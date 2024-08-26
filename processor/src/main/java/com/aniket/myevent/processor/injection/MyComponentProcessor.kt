package com.aniket.myevent.processor.injection

import com.aniket.myevent.annotations.MyComponent
import com.aniket.myevent.annotations.MyProvides
import com.aniket.myevent.processor.MethodReturnType
import com.aniket.myevent.processor.MyProcessorsTracker
import com.aniket.myevent.processor.extensions.KSPExtensions.containsAnnotation
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate


class MyComponentProcessor(
    options: Map<String, String>,
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator
) : SymbolProcessor {
    private val packageName = ""

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(MyComponent::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        val dependencies = Dependencies(
            aggregating = false,
            *resolver.getAllFiles().toList().toTypedArray()
        )

        createNewFile(dependencies)

        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach {
                it.accept(MyComponentVisitor(dependencies), Unit)
            }
        return unableToProcess.toList()
    }

    private fun createNewFile(dependencies: Dependencies) {
        try {
            val outputStream = codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName,
                fileName = "dummy",
                extensionName = "txt"
            )
            outputStream.use {
                it.write(
                    """//this file is created, here you go.
    """.trimMargin().toByteArray()
                )
            }
        } catch (e: FileAlreadyExistsException) {
            logger.warn("file already exists. ${e.file.name}")
        }
    }

    private inner class MyComponentVisitor(val dependencies: Dependencies) : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

            val name = classDeclaration.simpleName.getShortName()
            val toGenerateFileName = name + "Factory"
            val packageName = classDeclaration.packageName.asString()

//            val outputStream = codeGenerator.createNewFile(
//                dependencies = dependencies,
//                packageName,
//                fileName = toGenerateFileName
//            )

            MyProcessorsTracker.componentsQualifiedNames[name] =
                "${classDeclaration.qualifiedName?.asString()}"

            for (function in classDeclaration.getDeclaredFunctions()) {

                if (function.containsAnnotation(MyProvides::class.qualifiedName)) {
                    val functionReturnType = function.returnType?.element.toString()
                    val functionName = function.simpleName

                    MyProcessorsTracker.qualifiedReturnType[name].let {
                        val list = it ?: mutableListOf()
                        list.add(
                            MethodReturnType(
                                functionName.getShortName(),
                                functionReturnType
                            )
                        )
                        MyProcessorsTracker.qualifiedReturnType[name] = list
                    }

                }

            }

//            outputStream.use {
//                it.write(
//                    """
//                    |package $packageName
//                """.trimMargin().toByteArray()
//                )
//            }
        }
    }
}