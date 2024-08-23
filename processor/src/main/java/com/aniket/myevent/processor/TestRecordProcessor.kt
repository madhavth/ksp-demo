package com.aniket.myevent.processor

import com.aniket.myevent.annotations.Calculator
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.aniket.myevent.annotations.TestRecord
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import java.io.FileOutputStream
import java.io.OutputStream


@OptIn(DelicateKotlinPoetApi::class)
class TestRecordProcessor(
    options: Map<String, String>,
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(TestRecord::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        val dependencies = Dependencies(
            aggregating = false,
            *resolver.getAllFiles().toList().toTypedArray()
        )

        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach {
                it.accept(TestRecordVisitor(dependencies), Unit)
            }
        return unableToProcess.toList()
    }

    private inner class TestRecordVisitor(val dependencies: Dependencies) : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            super.visitClassDeclaration(classDeclaration, data)

            val packageName = classDeclaration.packageName.asString()
            val simpleName = classDeclaration.simpleName.getShortName()

            try {
                val outputStream = codeGenerator.createNewFile(
                    dependencies = dependencies,
                    packageName = packageName,
                    fileName = "ExtensionSame",
                )
                appendToFile(outputStream, simpleName, classDeclaration, "package $packageName\n")
            } catch (e: Exception) {
                if (e is FileAlreadyExistsException) {
                    val outputStream = FileOutputStream(e.file, true)
                    appendToFile(outputStream, simpleName, classDeclaration,"")
                    return
                }
                logger.error("exception occcurred $e")
            }
        }
    }

    fun appendToFile(outputStream: OutputStream, simpleName: String, classDeclaration: KSClassDeclaration, packageName: String) {
        val name = "${simpleName}Extension"

        val addAbstract = FunSpec.builder("add")
            .addParameter("a", Int::class)
            .addParameter("b", Int::class)
            .returns(Int::class)
            .addModifiers(KModifier.ABSTRACT)
            .build()


        val subtractAbstract = FunSpec.builder("subtract")
            .addParameter("a", Int::class)
            .addParameter("b", Int::class)
            .returns(Int::class)
            .addModifiers(KModifier.ABSTRACT)
            .build()


        val entireInterface = TypeSpec.interfaceBuilder(
            name + "CallBack"
        ).addFunction(subtractAbstract)
            .addFunction(addAbstract)
            .build()


        val addFunction = FunSpec.builder("add")
            .returns(Int::class)
            .addModifiers(KModifier.PRIVATE)
            .addParameter("a", Int::class)
            .addParameter("b", Int::class.java)
            .addStatement("return a + b")
            .build()


        val subtractFunction = FunSpec.builder("subtract")
            .returns(Int::class)
            .addModifiers(KModifier.OPEN)
            .addParameter("a", Int::class)
            .addParameter("b", Int::class)
            .addStatement("return a - b")
            .build()


        val entireClass = TypeSpec.classBuilder(name)
            .addAnnotation(Calculator::class.java)
            .addFunction(addFunction)
            .addModifiers(KModifier.OPEN)
            .addFunction(subtractFunction)
            .build()

        outputStream.write(
            """$packageName
                |${entireClass}
                |
                |${entireInterface}
            """.trimMargin().toByteArray()
        )
        outputStream.flush()
        outputStream.close()
    }
}