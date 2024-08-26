package com.aniket.myevent.processor

import com.aniket.myevent.annotations.MyFile
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
import java.io.FileInputStream
import java.io.FileOutputStream


class FileProcessor(
    options: Map<String, String>,
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator
) : SymbolProcessor {
    private val packageName = ""

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(MyFile::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        val dependencies = Dependencies(
            aggregating = false,
            *resolver.getAllFiles().toList().toTypedArray()
        )

        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach {
                it.accept(FileVisitor(dependencies), Unit)
            }
        return unableToProcess.toList()
    }

    private inner class FileVisitor(val dependencies: Dependencies) : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

            val name = classDeclaration.simpleName.getShortName()
            val toGenerateFileName = name + "Generated"
            val packageName = classDeclaration.packageName.asString()

            // read the file here and output saved to this outputStream
            val fileName = "test.txt"
            val fileContent: String? = this.javaClass.classLoader.getResourceAsStream("test.txt")
                ?.bufferedReader()
                ?.readLine()


            val outputStream = codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName,
                fileName = toGenerateFileName
            )

            outputStream.use {
                it.write(
                    """
                    |package $packageName
                    |
                    |//$fileContent
                    |
                """.trimMargin().toByteArray()
                )
            }
        }
    }
}

internal class Test {

}

fun main() {
    val test = Test()
    val fileName = "test.txt"

    val classLoader = test.javaClass.classLoader
    val resources = classLoader.getResources("test.txt")
    resources.asIterator().forEachRemaining { resource ->
        println("Available resource: ${resource.path}")
    }

}