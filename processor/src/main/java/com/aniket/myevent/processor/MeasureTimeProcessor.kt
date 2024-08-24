package com.aniket.myevent.processor

import com.aniket.myevent.annotations.MeasureTime
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import java.io.FileOutputStream
import java.io.OutputStream


class MeasureTimeProcessor(
    options: Map<String, String>,
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(MeasureTime::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        val dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray())

        symbols.filter { it is KSFunctionDeclaration && it.validate() }
            .forEach { it.accept(MeasureTimeVisitor(dependencies), Unit) }

        return unableToProcess.toList()
    }

    private inner class MeasureTimeVisitor(val dependencies: Dependencies) : KSVisitorVoid() {

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            val packageName = function.packageName.asString()

            val toGenerateFile = "MeasureTimeExtension"

            var outputStream: OutputStream? = null
            var fileExists = false
            try {
                outputStream = codeGenerator.createNewFile(
                    dependencies,
                    packageName,
                    fileName = toGenerateFile
                )
            } catch (e: Exception) {
                if (e is FileAlreadyExistsException) {
                    outputStream = FileOutputStream(e.file, true)
                    fileExists = true
                } else {
                    logger.error(e.message ?: "error occurred reading $toGenerateFile")
                }

            }
            writeToFile(function, outputStream, fileExists)
        }

        private fun writeToFile(
            function: KSFunctionDeclaration,
            outputStream: OutputStream?,
            isAppend: Boolean
        ) {
            val shortName = function.simpleName.getShortName()
            val packageName = function.packageName.asString()

            if (outputStream == null) {
                logger.error("found outputStream null for $shortName")
            }

            val copyFunction = FunSpec.builder("MeasuredTime${shortName}")

            //add parameters
            function.parameters.forEach {
                val type = it.type.resolve()

                val  variableName = it.name!!.getShortName()
                if(!it.isNotKotlinPrimitive()) {
                    copyFunction.addParameter(
                        variableName,
                        ClassName.bestGuess(it.getPrimitiveTypeName())
                    )
                    return@forEach
                }

                val declarationPackage = type.declaration.packageName.asString()
                val declarationName = type.declaration.simpleName.asString()

                if(variableName == "complexType") {
                    logger.error("current $declarationName is ${type.isFunctionType}")
                }

                val name = type.declaration.qualifiedName!!.getShortName()

                copyFunction.addParameter(
                    ParameterSpec.builder(
                        variableName,
                         ClassName.bestGuess(declarationPackage + "."
                                 + name
                         )
                    ).build()
                )

            }

            val paramPassed = function.parameters.map {
                it.name!!.getShortName()
            }.joinToString(",")
            copyFunction.addStatement("val startTime = System.currentTimeMillis()")
            // add remaining statements
            copyFunction.addStatement("val result = ${function.simpleName.asString()}($paramPassed)")
            copyFunction.addStatement("val endTime = System.currentTimeMillis()")
            copyFunction.addStatement(
                """println("$shortName took" + (endTime -startTime))
            """.trimIndent()
            )

            val fileOutput = StringBuilder()

            if (!isAppend) {
                val fs = FileSpec.builder(packageName = packageName, "")
                    .addImport(packageName, shortName)
                    .build()
                fileOutput.append(fs.toString())
            }

            outputStream!!.write(
                """$fileOutput
${copyFunction.build()}
            """.trimMargin().toByteArray()
            )

            outputStream.close()
        }
    }

    private fun Modifier.toKModifier(): KModifier? {
        return when (this) {
            Modifier.PUBLIC -> KModifier.PUBLIC
            Modifier.PRIVATE -> KModifier.PRIVATE
            Modifier.PROTECTED -> KModifier.PROTECTED
            Modifier.INTERNAL -> KModifier.INTERNAL
            Modifier.ABSTRACT -> KModifier.ABSTRACT
            Modifier.OVERRIDE -> KModifier.OVERRIDE
            Modifier.OPEN -> KModifier.OPEN
            Modifier.FINAL -> KModifier.FINAL
            else -> null
        }
    }
}