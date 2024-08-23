package com.aniket.myevent

import com.aniket.myevent.annotations.MyTest
import com.aniket.myevent.annotations.TestRecord

@MyTest
class ThisIsATestBuddy(val a: Int, val b: Int) {
}

fun testSomething() {
   val test = ThisIsATestBuddyTest(10,20)
   test.addIntegers()
}

@MyTest
class ThisIsAnotherTest(val a: Int)

@TestRecord
class TestingThisRecord() {

}
