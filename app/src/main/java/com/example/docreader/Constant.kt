package com.example.docreader

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Constant {
    var constant: Constant? = null

    fun getInstance(): Constant? {
        return if (constant == null) {
           Constant().also { constant = it }
        } else constant
    }

    fun checkMimeType(context: Context, url: Uri): String {
        val type : String = context.contentResolver.getType(url).toString()
        return if (type.substringBefore("/").equals("application")){
            type.substringAfter("/")
        } else {
            type.substringBefore("/")
        }
    }

    fun creteFile(filename: String) : File {
        val myDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        Log.e("creteFile", "$myDir MYdir ")
        val sdf = SimpleDateFormat("dd/M/yyyy_hh_mm_ss", Locale("IN"))
        val currentDate = sdf.format(Date())
        Log.e("creteFile",currentDate )
        val fname : String  = currentDate + "_"+ filename
        val createFilePath : File = File(myDir , fname)
        Log.e("creteFile", "$fname File name ")
        try {
            if (!myDir.isDirectory) {
                myDir.mkdirs()
            }
            createFilePath.createNewFile()
            Log.e("creteFile", "${createFilePath.absoluteFile} ")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("creteFile", e.localizedMessage + " Exception ")
        }
        return createFilePath
    }
}
