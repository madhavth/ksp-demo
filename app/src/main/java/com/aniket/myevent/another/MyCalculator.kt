package com.aniket.myevent.another

import com.aniket.myevent.annotations.Add
import com.aniket.myevent.annotations.Calculator
import com.aniket.myevent.annotations.Multiply
import com.aniket.myevent.annotations.SquareAdd
import com.aniket.myevent.annotations.Subtract

@Calculator
interface MyCalculator {

    @Add
    fun add(a: Int, b: Int, c: Int, d: Int): Int

    @Subtract
    fun subtract(a: Int, b: Int, c: Int, d: Int): Int

    @Multiply
    fun multiply(a: Int, b: Int): Int

    @Add
    fun noParam(): Int

    @SquareAdd
    fun squareAddThis(a: Int, b: Int): Int
}