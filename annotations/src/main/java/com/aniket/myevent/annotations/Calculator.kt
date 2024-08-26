package com.aniket.myevent.annotations

@Target(AnnotationTarget.CLASS)
annotation class Calculator()

@Target(AnnotationTarget.FUNCTION)
annotation class Add

@Target(AnnotationTarget.FUNCTION)
annotation class Subtract

@Target(AnnotationTarget.FUNCTION)
annotation class Multiply


@Target(AnnotationTarget.FUNCTION)
annotation class SquareAdd


@Target(AnnotationTarget.CLASS)
annotation class MyService

@Target(AnnotationTarget.FUNCTION)
annotation class MyGET(
    val value: String = "123"
)

// this is what my interface will look like