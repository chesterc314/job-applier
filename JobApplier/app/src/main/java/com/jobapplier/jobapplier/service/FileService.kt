package com.jobapplier.jobapplier.service

import java.io.File
import java.io.FileReader
import java.io.FileWriter

object FileService {
    fun writeFile(content: String, fileName: String, destinationFilePath: String, overrideFile: Boolean = false): File {
        val file = File("$destinationFilePath$fileName")
        val fileWriter = FileWriter(file)
        if (file.exists() && !overrideFile) {
            fileWriter.append(content)
        } else {
            fileWriter.write(content)
        }
        fileWriter.close()
        return file
    }

    fun readFile(filePath: String): List<String> {
        val file = File(filePath)
        val fileReader = FileReader(file)
        return fileReader.readLines()
    }
}