package com.aniket.myevent

import com.aniket.myevent.annotations.MyComponent
import com.aniket.myevent.annotations.MyProvides

@MyComponent
class AppComponent {

    @MyProvides
    fun providesString(): String {
        return "testing"
    }

    @MyProvides
    fun providesInteger(): Int {
        return 123456
    }
}