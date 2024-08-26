package com.aniket.myevent

import com.aniket.myevent.annotations.MyGET
import com.aniket.myevent.annotations.MyService

@MyService
interface TestingService {

    // extension function for
    @MyGET
    fun getSomething(): String

    @MyGET
    fun getInteger(): Int

    @MyGET
    fun getLong(): Long
}