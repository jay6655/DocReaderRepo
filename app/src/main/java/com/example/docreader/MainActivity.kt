package com.example.docreader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*

import com.aspose.words.Document;
import com.aspose.words.License;

class MainActivity : AppCompatActivity() {
    private val RECORD_REQUEST_CODE = 101
    private var bitmap: Bitmap? = null
    private var url:Uri? = null
    private var pdfFile:File? = null
    private var createFilePath :File ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.select)
        button?.setOnClickListener() {
            setupPermissions()
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            val i = Intent.createChooser(intent, "File")
            startActivityForResult(i, 111)
        }


        val convert = findViewById<Button>(R.id.convert)
        convert?.setOnClickListener() {
            val mimeTypeStr :String = checkMimeType()
            if ("image".equals(mimeTypeStr)) {
                createImageToPdf()
            }
            else if ("pdf".equals(mimeTypeStr)){
                //createPdfToImage()
                createPdfToDoc()
            }
            else {
                createDocToPdf()
            }
        }
    }

    private fun createPdfToDoc() {
        Log.e("ASD" , "createPdfToDoc  " + createFilePath.toString() + " " + pdfFile.toString() )
        val doc = Document(pdfFile.toString())
        // save DOCX as PDF
        creteFile("DocTOPdfFile.doc")
        doc.save(createFilePath.toString())
        // show PDF file location in toast as well as treeview (optional)
        Log.e("ASD" , " createPdfToDoc  " + createFilePath.toString() + " ")
    }

    private fun createDocToPdf() {
        Log.e("ASD" , " DOCX as PDF  " + createFilePath.toString() + " " + pdfFile.toString() )
        val doc = Document(pdfFile.toString())
        // save DOCX as PDF
        creteFile("DocTOPdfFile.pdf")
        doc.save(createFilePath.toString())
        // show PDF file location in toast as well as treeview (optional)
        Log.e("ASD" , " DOCX as PDF  " + createFilePath.toString() + " ")
    }

    private fun checkMimeType(): String {
        val type : String = contentResolver.getType(url!!).toString()
        Log.e("MYMEtype", type + " dd " + type.substringBefore("/"))
        return type.substringBefore("/")
    }

    private fun createPdfToImage() {
        val bitmaps: ArrayList<Bitmap> = ArrayList()
        try {
            Log.e("pdfFile", pdfFile!!.absolutePath + "  absolutePath ");
            Log.e("pdfFile", pdfFile.toString() + " ");
            val renderer = PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY))
            var bitmap: Bitmap
            val pageCount = renderer.pageCount
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = resources.displayMetrics.densityDpi / 72 * page.width
                val height = resources.displayMetrics.densityDpi / 72 * page.height
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                // close the page
                page.close()
                findViewById<ImageView>(R.id.image).setImageBitmap(bitmap);
            }

            // close the renderer
            renderer.close()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            Log.e("pdfFile", ex.localizedMessage + " Exception ");
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111){
            Log.e("TAG", data.toString())
            this.url = data!!.data
            val mimeTypeStr :String = checkMimeType()
            if ("image".equals(mimeTypeStr)) {
                val inStream: InputStream? = contentResolver.openInputStream(data!!.getData()!!)
                bitmap = BitmapFactory.decodeStream(inStream);
            }
            else {
                try {
                    val fiileUtil : FileUtil = FileUtil();
                    pdfFile = fiileUtil.convertFile(this@MainActivity, data.data!!)!!
                    Log.e("file", "File...:::: uti - " + pdfFile!!.path.toString() + " file -" + pdfFile.toString() + " : " + pdfFile!!.exists())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

        }
    }

    private fun createImageToPdf() {
        val document = PdfDocument()
        val pageInfo = PageInfo.Builder(bitmap!!.getWidth(), bitmap!!.getHeight(), 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        paint.setColor(Color.parseColor("#ffffff"))
        canvas.drawPaint(paint)
        bitmap = bitmap?.let { Bitmap.createScaledBitmap(bitmap!!, bitmap!!.getWidth(), it.getHeight(), true) }
        paint.setColor(Color.BLUE)
        canvas.drawBitmap(bitmap!!, 0f, 0f, paint)
        document.finishPage(page)

        creteFile("ImgToPdf.pdf")
        try {
            document.writeTo(FileOutputStream(createFilePath))
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Something wrong: " + e.toString(), Toast.LENGTH_LONG).show()
        }

        // close the document
        document.close()
    }

    private fun creteFile (filename : String){
        val dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        createFilePath = File(dirPath, filename)
        try {
            if (!dirPath.isDirectory) {
                dirPath.mkdirs()
            }
            createFilePath!!.createNewFile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.e("TAG", "Permission to record denied")
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    RECORD_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RECORD_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.e("TAG", "Permission has been denied by user")
                } else {
                    Log.e("TAG", "Permission has been granted by user")
                }
            }
        }
    }
}
