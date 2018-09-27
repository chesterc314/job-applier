package com.jobapplier.jobapplier.service

import java.io.File
import java.io.FileReader
import java.io.FileWriter

object FileService {
    fun writeFile(content: String, fileName: String, destinationFilePath: String, overrideFile: Boolean = false): File {
        val directory = File(destinationFilePath)
        if(!directory.exists()){
            directory.mkdir()
        }

        val file = File("$destinationFilePath/$fileName")
        val isAppendable = file.exists() && !overrideFile
        val fileWriter = FileWriter(file, isAppendable)
        if (isAppendable) {
            fileWriter.append(content)
        } else {
            fileWriter.write(content)
        }
        fileWriter.flush()
        fileWriter.close()
        return file
    }

    fun readFile(filePath: String): List<String> {
        val file = File(filePath)
        val fileReader = FileReader(file)
        return fileReader.readLines()
    }
}