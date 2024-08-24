package com.aniket.myevent.processor.calculator

import com.aniket.myevent.annotations.Add
import com.aniket.myevent.annotations.Calculator
import com.aniket.myevent.annotations.Multiply
import com.aniket.myevent.annotations.SquareAdd
import com.aniket.myevent.annotations.Subtract
import com.aniket.myevent.processor.extensions.KSPExtensions.containsAnnotationShortName
import com.aniket.myevent.processor.extensions.KSPExtensions.functionArguments
import com.aniket.myevent.processor.extensions.KSPExtensions.getClassReturnType
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

class CalculatorProcessor(
    options: Map<String, String>,
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator
) : SymbolProcessor {
    private val packageName = ""

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(Calculator::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        val dependencies = Dependencies(
            aggregating = false,
            *resolver.getAllFiles().toList().toTypedArray()
        )

        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach {
                it.accept(CalculatorVisitor(dependencies), Unit)
            }
        return unableToProcess.toList()
    }

    private inner class CalculatorVisitor(val dependencies: Dependencies) : KSVisitorVoid() {
        @OptIn(KspExperimental::class)
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

            val name = classDeclaration.simpleName.getShortName()
            val toGenerateFileName = name + "Generated"
            val packageName = classDeclaration.packageName.asString()

            val outputStream = codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName,
                fileName = toGenerateFileName
            )

            val classBuilder = TypeSpec.classBuilder(toGenerateFileName)

            val functions = classDeclaration.getDeclaredFunctions()
            for (function in functions) {
                var arguments = function.parameters.map { it.name?.getShortName() }

                if(arguments.isEmpty()) {
                    arguments = listOf("0")
                }

                processSymbol(function, Add::class.simpleName, classBuilder) {
                    add(arguments, it)
                }

                processSymbol(function, Subtract::class.simpleName, classBuilder) {
                    subtract(arguments, it)
                }

                processSymbol(function, Multiply::class.simpleName, classBuilder) {
                    multiply(arguments, it)
                }

                processSymbol(function, SquareAdd::class.simpleName, classBuilder) {
                    squareAdd(arguments,it)
                }
            }

            outputStream.use {
                it.write(
                    """
                    |package $packageName
                    |
                    |${classBuilder.build()}
                """.trimMargin().toByteArray()
                )
            }
        }

        private fun squareAdd(arguments: List<String?>, it: FunSpec.Builder) {
            val arg = arguments.map {
                "$it*$it"
            }.joinToString("+")
            it.addStatement("return $arg")
        }

        private fun multiply(arguments: List<String?>, it: FunSpec.Builder) {
            it.addStatement("return ${arguments.joinToString("*")}")
        }

        private fun add(
            arguments: List<String?>,
            it: FunSpec.Builder
        ) {
            it.addStatement("return ${arguments.joinToString("+")}")
        }

        private fun subtract(
            arguments: List<String?>,
            it: FunSpec.Builder
        ) {
            it.addStatement("return ${arguments.joinToString("-")}")
        }
    }


    @OptIn(KspExperimental::class)
    fun processSymbol(
        function: KSFunctionDeclaration,
        shortName: String?,
        classDeclaration: TypeSpec.Builder,
        callback: (FunSpec.Builder) -> Unit,
                      ) {
        val containsAnnotation = function.containsAnnotationShortName(shortName)
        if (containsAnnotation) {
            val funSpec = FunSpec.builder(function.simpleName.getShortName())
            // check if has return type if has then return it
            var doesReturn: Boolean = false

            function.returnType?.element?.let {
                doesReturn = true
                funSpec.returns(
                    function.getClassReturnType()!!
                )
            }

            // add all the params as they are
            funSpec.addParameters(function.functionArguments())
            funSpec.addStatement("// auto generated code for $shortName")
            callback(funSpec)
            // add the body and return statement
            classDeclaration.addFunction(funSpec.build())
        }
    }

}