package com.aniket.myevent

import com.aniket.myevent.annotations.Calculator
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitMyTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testQualifierName() {
        val value = Calculator::class.qualifiedName
        assertEquals(value, "Calculator")
    }
}