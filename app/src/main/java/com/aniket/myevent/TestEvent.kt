package com.aniket.myevent

import com.aniket.myevent.annotations.MyTest
import com.aniket.myevent.annotations.TestRecord

@MyTest
class ThisIsATestBuddy(val a: Int, val b: Int) {
}

@MyTest
class ThisIsAnotherTest(val a: Int)

@TestRecord
class TestingThisRecord() {

}
