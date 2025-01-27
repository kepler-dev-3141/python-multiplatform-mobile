package python.multiplatform.ffi


expect object Python3 {
    var isInitialized: Boolean
        private set
    fun initialize()
    fun initialize_ex(initsigs: Int)

    fun finalize()
    //val builtins: Builtins

    fun pyLongFromLong(long: Int): UInt
    fun pyLongAsLong(pyLong: UInt): Int

//    fun PyRun_SimpleString(command: UInt): Int
//    fun PyUnicode_FromString(str: String): UInt
}
