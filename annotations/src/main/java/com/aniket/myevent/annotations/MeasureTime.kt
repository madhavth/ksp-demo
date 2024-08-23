package com.aniket.myevent.annotations

import java.lang.annotation.RetentionPolicy

@Target(AnnotationTarget.FUNCTION)
annotation class MeasureTime


// for now planning on global function
// add annotation
// start time imported and logged
// function returns something ? -- will check afterwards
// happy path: function doesnt return anything
// log ends