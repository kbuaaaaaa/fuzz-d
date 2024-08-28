package fuzzd.validator.executor.execution_handler

import fuzzd.utils.compareVersions

class JavaExecutionHandler(fileDir: String, fileName: String, dafnyVersion: String) : AbstractExecutionHandler(fileDir, fileName, dafnyVersion=dafnyVersion) {
    override fun getCompileTarget(): String = "java"

    override fun getExecuteCommand(fileDir: String, fileName: String): String = if (compareVersions(dafnyVersion, "3.11.0") >= 0) {
        "java -jar $fileDir/$fileName.jar"
    } else {
        "java -cp $fileDir/$fileName-java:$fileDir/$fileName-java/DafnyRuntime.jar $fileDir/$fileName-java/$fileName.java"
    }
}
