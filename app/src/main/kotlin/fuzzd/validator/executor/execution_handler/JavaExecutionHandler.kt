package fuzzd.validator.executor.execution_handler

class JavaExecutionHandler(fileDir: String, fileName: String) : AbstractExecutionHandler(fileDir, fileName) {
    override fun getCompileTarget(): String = "java"

    override fun getExecuteCommand(fileDir: String, fileName: String): String =
        "java -jar $fileDir/$fileName.jar"
}
