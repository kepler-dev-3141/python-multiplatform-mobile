package python.multiplatform.ffi

import jdk.incubator.foreign.*
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType


actual object Python3 {
    private val scope
        get() = ResourceScope.newSharedScope()

    // 초기화 및 종료
    private val pyInitializeHandle: MethodHandle
    private val pyInitializeExHandle: MethodHandle
//    private val pyInitializeFromConfigHandle: MethodHandle
    private val pyFinalizeHandle: MethodHandle
    //private val pyFinalizeExHandle: MethodHandle
    private val pyIsInitializedHandle: MethodHandle

//    // 예외 처리
//    private val pyErrOccurredHandle: MethodHandle
//    private val pyErrPrintHandle: MethodHandle
//    private val pyErrClearHandle: MethodHandle
//
//    // 기타
//    private val pyRunSimpleStringHandle: MethodHandle
//    private val pyEvalGetBuiltinsHandle: MethodHandle
//
//    // 모듈 및 객체 관리
//    private val pyImportImportModuleHandle: MethodHandle
//    private val pyObjectGetAttrStringHandle: MethodHandle
//    private val pyObjectHasAttrStringHandle: MethodHandle
//    private val pyObjectCallObjectHandle: MethodHandle
//    private val pyObjectCallFunctionObjArgsHandle: MethodHandle
//    private val pyIncRefHandle: MethodHandle
//    private val pyDecRefHandle: MethodHandle
//
//    // 타입 변환
    private val pyLongFromLongHandle: MethodHandle
    private val pyLongAsLongHandle: MethodHandle
//    private val pyFloatFromDoubleHandle: MethodHandle
//    private val pyFloatAsDoubleHandle: MethodHandle
//    private val pyUnicodeFromStringHandle: MethodHandle
//    private val pyUnicodeAsUTF8Handle: MethodHandle
//
//    // 컬렉션
//    private val pyListNewHandle: MethodHandle
//    private val pyListSizeHandle: MethodHandle
//    private val pyListGetItemHandle: MethodHandle
//    private val pyListSetItemHandle: MethodHandle
//    private val pyTupleNewHandle: MethodHandle
//    private val pyTupleSizeHandle: MethodHandle
//    private val pyTupleGetItemHandle: MethodHandle
//    private val pyTupleSetItemHandle: MethodHandle

//    private val PyRun_SimpleStringHandle: MethodHandle


    actual var isInitialized: Boolean = false
        private set

    actual fun initialize() {
        if (isInitialized) return
        scope.run {
            pyInitializeHandle.invokeExact()
            if (pyIsInitializedHandle.invokeExact() as Int == 0) {
                // TODO: Add error handling
                throw IllegalStateException("Python initialization failed")
            }
            println("INFO: Python initialized successfully!")
            isInitialized = true
        }
    }

    actual fun initialize_ex(initsigs: Int) { // Not tested
        if (isInitialized) return
        scope.run {
            pyInitializeExHandle.invokeExact(initsigs)
            if (pyIsInitializedHandle.invokeExact() as Int == 0) {
                // TODO: Add error handling
                throw IllegalStateException("Python initialization failed")
            }
            println("INFO: Python initialized successfully!")
            isInitialized = true
        }
    }

    actual fun finalize() {
        if (!isInitialized) return
        scope.run {
            pyFinalizeHandle.invokeExact()
            // TODO: print error message if exists
            if (pyIsInitializedHandle.invokeExact() as Int != 0) {
                throw IllegalStateException("Python finalization failed")
            }
            println("INFO: Python finalized successfully!")
            isInitialized = false
        }
    }

    actual fun pyLongFromLong(long: Int): UInt {
        val nativeAddress = pyLongFromLongHandle.invokeExact(long) as Int
        return nativeAddress as UInt
    }

    actual fun pyLongAsLong(pyLong: UInt): Int {
        return pyLongAsLongHandle.invokeExact(pyLong) as Int
    }

//    actual fun PyUnicode_FromString(str: String): UInt {
//        val nativeAddress = CLinker.toCString(str, scope).address()
//        return pyUnicodeFromStringHandle.invokeExact(nativeAddress) as UInt
//    }
//
//    actual fun PyRun_SimpleString(command: UInt): Int {
//        return PyRun_SimpleStringHandle.invokeExact(command) as Int
//    }

    init {
        scope.run {
            val lookup = MethodLookup(LibPythonManager::loadLibPython)

            // 초기화 및 종료
            pyInitializeHandle = lookup.find("Py_Initialize", Void.TYPE)
            pyInitializeExHandle = lookup.find("Py_InitializeEx", Void.TYPE, Integer.TYPE)
//            pyInitializeFromConfigHandle = lookup.find("Py_InitializeFromConfig", )
            pyFinalizeHandle = lookup.find("Py_Finalize", Void.TYPE)
            pyIsInitializedHandle = lookup.find("Py_IsInitialized", Integer.TYPE)

            // 타입 변환
            pyLongFromLongHandle = lookup.find("PyLong_FromLong", java.lang.Long.TYPE, Integer.TYPE)
            pyLongAsLongHandle = lookup.find("PyLong_AsLong", Integer.TYPE, java.lang.Long.TYPE)

//            pyUnicodeFromStringHandle = lookup.find("PyUnicode_FromString", Integer.TYPE, Integer.TYPE)
//            PyRun_SimpleStringHandle = lookup.find("PyRun_SimpleString", Integer.TYPE, Integer.TYPE)
        }
    }
}


internal class MethodLookup(libLoader: () -> Any) {
    private val symbolLookup: SymbolLookup
    private val linker: CLinker

    init {
        libLoader()
        symbolLookup = SymbolLookup.loaderLookup()
        linker = CLinker.getInstance()
    }

    fun find(symbol: String, returnType: Class<*>, vararg params: Class<*>): MethodHandle {
        val isParamRequired = params.isNotEmpty()
        val descriptor = if (returnType == Void::class.javaPrimitiveType) {
            if (isParamRequired) FunctionDescriptor.ofVoid(*params.map { it.toLayout() }.toTypedArray())
            else FunctionDescriptor.ofVoid()
        } else {
            val returnLayout = returnType.toLayout()
            if (isParamRequired) FunctionDescriptor.of(returnLayout, *params.map { it.toLayout() }.toTypedArray())
            else FunctionDescriptor.of(returnLayout)
        }
        return linker.downcallHandle(
            symbolLookup.lookup(symbol).get(),
            MethodType.methodType(returnType, params.map { it }.toTypedArray()),
            descriptor
        )
    }

    private fun Class<*>.toLayout(): ValueLayout {
        return when (this) {
            Long::class.javaPrimitiveType -> CLinker.C_POINTER

            Byte::class.javaPrimitiveType -> CLinker.C_CHAR
            Short::class.javaPrimitiveType -> CLinker.C_SHORT
            Int::class.javaPrimitiveType -> CLinker.C_INT
            Long::class.javaPrimitiveType -> CLinker.C_LONG
            Float::class.javaPrimitiveType -> CLinker.C_FLOAT
            Double::class.javaPrimitiveType -> CLinker.C_DOUBLE
            else -> throw IllegalArgumentException("Unsupported type for C ValueLayout conversion: $this")
        }
    }
}