package com.aniket.myevent.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.aniket.myevent.annotations.MyTest
import com.aniket.myevent.annotations.TestRecord
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


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
                appendToFile(outputStream, simpleName, classDeclaration, "package $packageName")
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
        outputStream.write(
            """$packageName
// this class is for $simpleName
class $name {
    private val packageName = "${classDeclaration.packageName.asString()}"
}

        """.trimMargin().toByteArray()
        )
        outputStream.flush()
        outputStream.close()
    }
}