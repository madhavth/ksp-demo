package com.aniket.myevent

import com.aniket.myevent.annotations.MyTest

@MyTest
class ThisIsATestBuddy(val a: Int, val b: Int) {
}

fun testSomething() {
   val test = ThisIsATestBuddyTest(10,20)
   test.addIntegers()
}