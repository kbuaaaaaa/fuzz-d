package fuzzd.utils

import java.io.File
import java.lang.ProcessBuilder.Redirect

const val DAFNY_ADVANCED = "advanced"
const val DAFNY_MAIN = "main"
const val DAFNY_TYPE = "dfy"
const val DAFNY_GENERATED = "generated"

fun runCommand(command: String): Process {
    return Runtime.getRuntime().exec(command)
}

fun compileDafny(targetLanguage: String, fileDir: String, fileName: String, timeout: Long): Process {
    val processBuilder = ProcessBuilder(
        "timeout",
        timeout.toString(),
        "dafny",
        "build",
        "$fileDir/$fileName.dfy",
        "-t",
        "$targetLanguage",
        "--no-verify",
        "--allow-warnings"
    )
    return processBuilder.start()
}

fun verifyDafny(fileDir: String, fileName: String, timeout: Long): Process {
    val command = "timeout $timeout dafny /compile:0 $fileDir/$fileName.dfy"
    return runCommand(command)
}

fun Process.readInputStream(): String = String(inputStream.readAllBytes())

fun Process.readErrorStream(): String = String(errorStream.readAllBytes())
