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
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*

import com.aspose.words.Document

class MainActivity : AppCompatActivity() ,  AdapterView.OnItemSelectedListener {
    private val _requestCode = 101
    private var bitmap: Bitmap? = null
    private var url:Uri? = null
    private var pdfFile:File? = null
    private val constant : Constant = Constant().getInstance()!!
    private var convertFileType :String = "ABC"
    private var spinner: Spinner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.select)
        button?.setOnClickListener {
            setupPermissions()
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            val i = Intent.createChooser(intent, "File")
            startActivityForResult(i, 111)
        }

        spinner = findViewById(R.id.selection)
        spinner!!.onItemSelectedListener = this


        val convert = findViewById<Button>(R.id.convert)
        convert?.setOnClickListener {
            val mimeTypeStr :String = constant.checkMimeType(this@MainActivity, this.url!!)
            Log.e("ConvertClick", "$mimeTypeStr convertFileType : $convertFileType");
            if ("image" == mimeTypeStr && convertFileType == "PDF" ) {
                createImageToPdf()
            }
            else if ("pdf" == mimeTypeStr && convertFileType == "DOC"){
                createPdfToDoc()
            }
            else if ("pdf" == mimeTypeStr && convertFileType == "IMG"){
                createPdfToImage()
            }
            else if ("msword" == mimeTypeStr && convertFileType == "PDF"){
                createDocToPdf()
            }
        }
    }

    private fun setSpinnerAdapter(toConvert: Int) {
        Log.e("setSpinnerAdapter" , toConvert.toString())
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(this,  toConvert , android.R.layout.simple_spinner_item)
                .also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner!!.adapter = adapter
                }
    }


    private fun createPdfToDoc() {
        Log.e("createPdfToDoc" , "createPdfToDoc  " + pdfFile.toString() )
        val doc = Document(pdfFile.toString())
        val createFilePath : File = constant.creteFile("DocTOPdfFile.doc")
        doc.save(createFilePath.toString())
        Log.e("createPdfToDoc" , " createPdfToDoc  $createFilePath ")
    }

    private fun createPdfToImage() {
        val bitmaps: ArrayList<Bitmap> = ArrayList()
        try {
            Log.e("createPdfToImage", pdfFile!!.absolutePath + "  absolutePath ")
            Log.e("createPdfToImage", pdfFile.toString() + " ")
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
                findViewById<ImageView>(R.id.image).setImageBitmap(bitmap)
            }

            // close the renderer
            renderer.close()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111){
            Log.e("onActivityResult", data.toString())
            this.url = data!!.data
            when (constant.checkMimeType(this@MainActivity, this.url!!)) {
                "image" -> {
                    setSpinnerAdapter(R.array.img_to_convert)
                    val inStream: InputStream? = contentResolver.openInputStream(data.data!!)
                    bitmap = BitmapFactory.decodeStream(inStream)
                }
                "pdf" -> {
                    setSpinnerAdapter(R.array.pdf_to_convert)
                    try {
                        val fiileUtil = FileUtil()
                        pdfFile = fiileUtil.convertFile(this@MainActivity, data.data!!)
                        Log.e("onActivityResult", "File...:::: uti - " + pdfFile!!.path.toString() + " file -" + pdfFile.toString() + " : " + pdfFile!!.exists())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                "msword" -> {
                    setSpinnerAdapter(R.array.doc_to_convert)
                    try {
                        val fiileUtil = FileUtil()
                        pdfFile = fiileUtil.convertFile(this@MainActivity, data.data!!)
                        Log.e("onActivityResult", "File...:::: uti - " + pdfFile!!.path.toString() + " file -" + pdfFile.toString() + " : " + pdfFile!!.exists())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun createImageToPdf() {
        val document = PdfDocument()
        val pageInfo = PageInfo.Builder(bitmap!!.width, bitmap!!.height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        paint.color = Color.parseColor("#ffffff")
        canvas.drawPaint(paint)
        bitmap = bitmap?.let { Bitmap.createScaledBitmap(bitmap!!, bitmap!!.width, it.height, true) }
        paint.color = Color.BLUE
        canvas.drawBitmap(bitmap!!, 0f, 0f, paint)
        document.finishPage(page)

        val createFilePath : File = constant.creteFile("ImgToPdf.pdf")
        try {
            document.writeTo(FileOutputStream(createFilePath))
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Something wrong: $e", Toast.LENGTH_LONG).show()
        }
        document.close()
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.e("setupPermissions", "Permission to record denied")
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    _requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            _requestCode -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.e("PermissionsResult", "Permission has been denied by user")
                } else {
                    Log.e("PermissionsResult", "Permission has been granted by user")
                }
            }
        }
    }

    private fun createDocToPdf() {
        Log.e("createDocToPdf" , " DOCX as PDF  " + pdfFile.toString() )
        val doc = Document(pdfFile.toString())
        val createFilePath : File = constant.creteFile("DocTOPdfFile.pdf")
        doc.save(createFilePath.toString())
        Log.e("createDocToPdf" , " DOCX as PDF  $createFilePath ")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val themes : Array<String>
        when (constant.checkMimeType(this@MainActivity, this.url!!)) {
            "image" -> {
                themes = resources.getStringArray(R.array.img_to_convert)
                convertFileType = themes[position]
                Log.e("onItemSelected" , themes[position] + " dd " )
            }
            "pdf" -> {
                themes = resources.getStringArray(R.array.pdf_to_convert)
                convertFileType = themes[position]
                Log.e("onItemSelected" , themes[position] + " dd " )
            }
            "msword" -> {
                themes = resources.getStringArray(R.array.doc_to_convert)
                convertFileType = themes[position]
                Log.e("onItemSelected" , themes[position] + " dd " )
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}
