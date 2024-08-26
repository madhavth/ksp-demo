package com.aniket.myevent.annotations


@Target(AnnotationTarget.FIELD)
annotation class CustomInject


@Target(AnnotationTarget.CLASS)
annotation class MyComponent

@Target(AnnotationTarget.FUNCTION)
annotation class MyProvides