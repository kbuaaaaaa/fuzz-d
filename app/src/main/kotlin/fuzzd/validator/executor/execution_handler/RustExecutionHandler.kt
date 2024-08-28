package fuzzd.validator.executor.execution_handler

class RustExecutionHandler(fileDir: String, fileName: String, dafnyVersion: String) : AbstractExecutionHandler(fileDir, fileName, dafnyVersion=dafnyVersion) {
    override fun getCompileTarget(): String = "rs"

    override fun getExecuteCommand(fileDir: String, fileName: String): String = "$fileDir/$fileName-rust/target/debug/$fileName"
}