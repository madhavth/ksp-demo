package com.aniket.myevent.processor.injection

import com.aniket.myevent.annotations.CustomInject
import com.aniket.myevent.annotations.MyClass
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import java.lang.reflect.Type


var runningOrder: MutableList<String> = mutableListOf()

class InjectionProcessor(
    options: Map<String, String>,
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator
) : SymbolProcessor {
    private val packageName = ""

    override fun process(resolver: Resolver): List<KSAnnotated> {
        runningOrder.add(this::class.java.simpleName)
        val symbols = resolver
            .getSymbolsWithAnnotation(MyClass::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        val dependencies = Dependencies(
            aggregating = false,
            *resolver.getAllFiles().toList().toTypedArray()
        )

        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach {
                it.accept(InjectionVisitor(dependencies), Unit)
            }

        return unableToProcess.toList()
    }

    private inner class InjectionVisitor(val dependencies: Dependencies) : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

            val name = classDeclaration.simpleName.getShortName()
            val toGenerateFileName = name + "Inject"
            val packageName = classDeclaration.packageName.asString()

            val outputStream = codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName,
                fileName = toGenerateFileName
            )

            val classBuilder = TypeSpec.classBuilder(toGenerateFileName)

            val injectBuilder = FunSpec.builder("inject")
                .addParameter("instance", ClassName.bestGuess(classDeclaration.qualifiedName?.asString()?:""))

            val companionBuilder = TypeSpec.companionObjectBuilder()

            for(property in classDeclaration.getDeclaredProperties()) {
                // get property type
                val propertyName = property.simpleName.asString()
                val propertyType = property.type.element.toString()

                val statement: String = when(propertyType) {
                    "String" -> {
                        """ "testing this works" """
                    }
                    "Int" -> {
                        """ 77 """
                    }
                    else -> {""" null """}
                }

                injectBuilder.addCode(""" instance.${propertyName} = $statement """)
                injectBuilder.addCode("""// property $propertyName is of type $propertyType
                    |
                """.trimMargin())
            }

            // build companion Builder
            companionBuilder.addFunction(injectBuilder.build())
            classBuilder.addType(companionBuilder.build())

            outputStream.use {
                it.write(
                    """
                    |package $packageName
                    |
                    |${classBuilder.build()}
                    |// ${runningOrder.joinToString(",")}
                """.trimMargin().toByteArray()
                )
            }
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            super.visitPropertyDeclaration(property, data)
            val className = property.closestClassDeclaration()
            if(className == null) {
                logger.error("not delcared inside class invalid statement")
                return
            }
        }
    }
}