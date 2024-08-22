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

fun compileDafny(targetLanguage: String, fileDir: String, fileName: String, timeout: Long, older: Int): Process {
    val processBuilder = when (older) {
        0 -> ProcessBuilder(
        "timeout",
        timeout.toString(),
        "dafny",
        "build",
        "$fileDir/$fileName.dfy",
        "-t",
        "$targetLanguage",
        "--no-verify",
        "--allow-warnings",
        "--function-syntax:4",
        )
        1 -> ProcessBuilder(
        "timeout",
        timeout.toString(),
        "dafny",
        "build",
        "$fileDir/$fileName.dfy",
        "-t",
        "$targetLanguage",
        "--no-verify",
        "--function-syntax:4",
        )
        2 -> ProcessBuilder(
        "timeout",
        timeout.toString(),
        "dafny",
        "/compileVerbose:0",
        "/noVerify",
        "/compile:2",
        "/spillTargetCode:1",
        "/compileTarget:$targetLanguage",
        "/functionSyntax:4",
        "$fileDir/$fileName.dfy",
        )
        else -> throw IllegalArgumentException("Invalid Dafny version")
    }  
    return processBuilder.start()
}

fun verifyDafny(fileDir: String, fileName: String, timeout: Long): Process {
    val command = "timeout $timeout dafny /compile:0 $fileDir/$fileName.dfy"
    return runCommand(command)
}

fun Process.readInputStream(): String = String(inputStream.readAllBytes())

fun Process.readErrorStream(): String = String(errorStream.readAllBytes())
