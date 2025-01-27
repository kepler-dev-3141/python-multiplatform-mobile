package org.thisisthepy.python.multiplatform.demo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import python.multiplatform.ffi.Python3


fun main() = application {
    Python3.initialize()
    val a = Python3.pyLongFromLong(1000)
    println(a)
    println(Python3.pyLongAsLong(a))

//    val hello = Python3.PyUnicode_FromString("hello")
//    println(hello)
//    Python3.PyRun_SimpleString(hello)
    Window(
        onCloseRequest = { Python3.finalize(); exitApplication() },
        title = "PythonMultiplatform",
    ) {
        App()
    }
}
