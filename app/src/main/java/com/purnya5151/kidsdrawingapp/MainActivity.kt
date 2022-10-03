package com.purnya5151.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    var CoustomProgressDialog: Dialog? = null

    val openGalleryLauncher : ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
        result ->
        if (result.resultCode == RESULT_OK && result.data != null){
            val imageBackground:ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageURI(result.data?.data)
        }
    }

    val requestPermission : ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){
        permissions ->
        permissions.entries.forEach{
            val permissionName = it.key
            val isGranted = it.value

            if (isGranted){
//                Toast.makeText(this,"Permission Granted now yo can read the internal storage.",
//                    Toast.LENGTH_LONG
//                ).show()

                val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)
            }else{
                if(permissionName== Manifest.permission.READ_EXTERNAL_STORAGE){
                    Toast.makeText(this,"Oops you just denied the permission.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        val linearlayoutPaintColor = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linearlayoutPaintColor[4] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
           ContextCompat.getDrawable(this,R.drawable.pallete_pressed)

        )

        drawingView?.setSizeFORBrush(20.toFloat())

        var ib_brush: ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener() {
            showBrushSizeChooserDialog()
        }
        var ib_undo: ImageButton = findViewById(R.id.ib_undo)
        ib_undo.setOnClickListener() {
            drawingView?.onClickUndoPath()

        }
        var ibsave: ImageButton = findViewById(R.id.ib_save)
        ibsave.setOnClickListener() {

            if (isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView:FrameLayout = findViewById(R.id.fl_drawing_view_container)

                    saveBitmapFile(getBitmapView(flDrawingView))
                }
            }



        }

        val ibGallery : ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener(){
            requestStoragePermission()
        }

    }

    private fun showBrushSizeChooserDialog() {
        val BrushDialog = Dialog(this)
        BrushDialog.setContentView(R.layout.dialog_brush_size)
        BrushDialog.setTitle("Brush Size: ")
        val smallBtn: ImageButton = BrushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener() {
            drawingView?.setSizeFORBrush(10.toFloat())
            BrushDialog.dismiss()
        }
        val mediumBtn: ImageButton = BrushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener() {
            drawingView?.setSizeFORBrush(20.toFloat())
            BrushDialog.dismiss()
        }
        val largeBtn: ImageButton = BrushDialog.findViewById(R.id.ib_large_brush)
       largeBtn.setOnClickListener() {
            drawingView?.setSizeFORBrush(35.toFloat())
            BrushDialog.dismiss()
        }
        BrushDialog.show()


    }

    fun paintClicked(view: View){
       if (view!== mImageButtonCurrentPaint){
           val imageButton = view as ImageButton
           val colorTag = imageButton.tag.toString()
           drawingView?.setColor(colorTag)
           imageButton.setImageDrawable(
               ContextCompat.getDrawable(this,R.drawable.pallete_pressed)

           )
           mImageButtonCurrentPaint?.setImageDrawable(
               ContextCompat.getDrawable(this,R.drawable.pallete_normal)
           )
           mImageButtonCurrentPaint = view

       }

    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE
            )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationalDialog("Kids Drawing App", "Kids Drawing App need access to your internal storage" +
                    "to assign background Image")
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE

            ))
        }
    }


    private fun getBitmapView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,
            view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable !=null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String{
        var result = " "
        withContext(Dispatchers.IO){
            if (mBitmap!=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90, bytes)
                    val f = File(externalCacheDir?.absoluteFile.toString()
                    + File.separator + "KidsDrawingApp_" + System.currentTimeMillis()/1000 + ".png" )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result= f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"File Saved successfully : $result",
                                Toast.LENGTH_SHORT).show()
                            shareFile(result)
                    }else{
                    Toast.makeText(this@MainActivity,"Something went wrong while saving the file. ", Toast.LENGTH_SHORT).show()
                }
                }
            }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()

                }
            }
        }

        return result
    }

    private fun showRationalDialog(title: String, message : String){
        val builder : AlertDialog.Builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){
                dialog, _-> dialog.dismiss()
            }
        builder.create().show()
    }

    private fun showProgressDialog(){
        CoustomProgressDialog = Dialog(this@MainActivity)

        CoustomProgressDialog?.setContentView(R.layout.dialog_coustom_progress)

        CoustomProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(CoustomProgressDialog!= null){
            CoustomProgressDialog?.dismiss()
            CoustomProgressDialog = null
        }
    }

    private fun shareFile(result:String){
            MediaScannerConnection.scanFile(this, arrayOf(result),null){
                path,uri ->
                val ShareIntent = Intent()
                ShareIntent.action = Intent.ACTION_SEND
                ShareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                ShareIntent.type = "image/png"
                startActivity(Intent.createChooser(ShareIntent,"Share"))
            }
    }


}