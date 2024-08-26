package com.aniket.myevent.processor

import com.aniket.myevent.annotations.MyFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import java.io.File
import kotlin.math.log


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
            val dbInputStream = this.javaClass.classLoader.getResourceAsStream("Car_Database.db")

//            dbInputStream?.copyTo(outputFile.outputStream())

            val exists = File(fileName).exists()

            if(exists) {
                logger.error("Car_Database.db not found.")
                return
            }

            val outputFile = File("car_db.db")
            val fileOutputStream = outputFile.outputStream()

            val outputStream = codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName = packageName,
                fileName = toGenerateFileName
            )

            dbInputStream?.copyTo(fileOutputStream)
            val sqliteWrapper = SQLiteWrapper(outputFile.canonicalPath)

            if(!outputFile.exists()) {
                logger.error("this is not what we agreed upon")
                return
            }

            val tables = sqliteWrapper.listTables()
            tables.forEach { println(it) }
            sqliteWrapper.close()




            outputStream.use {
                it.write(
                    """
                    |package $packageName
                    |
                    |//$tables
                    |
                """.trimMargin().toByteArray()
                )
            }
        }
    }
}