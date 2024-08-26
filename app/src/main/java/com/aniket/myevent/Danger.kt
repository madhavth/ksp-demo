package com.aniket.myevent

import com.aniket.myevent.annotations.CustomInject
import com.aniket.myevent.annotations.MyClass

@MyClass
class Danger {
    @CustomInject
    lateinit var testing: String

    @CustomInject
    var myInt: Int = 0

    @CustomInject
    lateinit var test: TestingThisRecord

    init {
        DangerInject.inject(this)
    }
}

fun main() {
    val danger = Danger()
    println(danger.testing)
    println(danger.myInt)
    println(danger.test)
}