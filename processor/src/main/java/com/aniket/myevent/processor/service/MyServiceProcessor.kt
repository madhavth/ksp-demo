package com.aniket.myevent.processor.service

import com.aniket.myevent.annotations.MyGET
import com.aniket.myevent.annotations.MyService
import com.aniket.myevent.processor.extensions.KSPExtensions.containsAnnotation
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec


class MyServiceProcessor(
    options: Map<String, String>,
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator
) : SymbolProcessor {
    private val packageName = ""

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(MyService::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        val dependencies = Dependencies(
            aggregating = false,
            *resolver.getAllFiles().toList().toTypedArray()
        )

        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach {
                it.accept(MyServiceVisitor(dependencies), Unit)
            }
        return unableToProcess.toList()
    }

    private inner class MyServiceVisitor(val dependencies: Dependencies) : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

            if(classDeclaration.classKind != ClassKind.INTERFACE) {
                logger.error("MyService annotated ${classDeclaration.simpleName.getShortName()} is not an interface")
                return
            }

            val name = classDeclaration.simpleName.getShortName()
            val toGenerateFileName = name + "Implemented"
            val packageName = classDeclaration.packageName.asString()

            val outputStream = codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName,
                fileName = toGenerateFileName
            )


            val classFunc = TypeSpec.classBuilder(toGenerateFileName)
                .addSuperinterface(ClassName.bestGuess(classDeclaration.qualifiedName!!.asString()))

            // visit all declared properties

            // get MyGET, get other annotations and process
            val myGetAnnotationFunctions = classDeclaration.getDeclaredFunctions()
                .filter {
                    it.containsAnnotation(MyGET::class.qualifiedName)
                }.toList()

            // process my GET annotation
            for(function in myGetAnnotationFunctions) {
                val myGETAnnotation =  function.annotations.first {
                    it.annotationType.resolve().declaration
                        .qualifiedName?.asString() == MyGET::class.qualifiedName
                }

                val argument = myGETAnnotation.arguments.first {
                    it.name?.getShortName() == "value"
                }

                val value = argument.value as? String

                // need to generate a function for each of these.
                val functionName = function.simpleName.getShortName()

                val funType = FunSpec.builder(functionName)
                    .addModifiers(KModifier.OVERRIDE)

                val returnType = function.returnType?.element?.toString()


                val assignment = when(returnType) {
                    "String" -> "\"$value this is it \""
                    "Int" -> "77"
                    "Long" -> "77L"
                    else -> "null"
                }

                if(returnType != null) {
                    funType.returns(ClassName.bestGuess(returnType))
                    funType.addStatement("//$returnType \nreturn $assignment")
                }

                classFunc.addFunction(funType.build())
            }



            outputStream.use {
                it.write(
                    """
                    |package $packageName
                    |
                    |${classFunc.build()}
                """.trimMargin().toByteArray()
                )
            }
        }
    }
}