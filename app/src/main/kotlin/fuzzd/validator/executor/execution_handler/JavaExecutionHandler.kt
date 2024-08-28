package fuzzd.validator.executor.execution_handler

class JavaExecutionHandler(fileDir: String, fileName: String, older: Int) : AbstractExecutionHandler(fileDir, fileName, older=older) {
    override fun getCompileTarget(): String = "java"

    override fun getExecuteCommand(fileDir: String, fileName: String): String = if (older < 2) {
        "java -jar $fileDir/$fileName.jar"
    } else {
        "java -cp $fileDir/$fileName-java:$fileDir/$fileName-java/DafnyRuntime.jar $fileDir/$fileName-java/$fileName.java"
    }
}
