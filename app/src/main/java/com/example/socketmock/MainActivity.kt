package com.example.socketmock

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.ArrayList
import com.example.socketmock.R;
import org.json.JSONArray
import org.json.JSONException

class MainActivity : AppCompatActivity() {

    lateinit var mSocket: Socket

    val REQUEST_GALLERY_PICK = 1

    private val PERMISSION_CALLBACK_CONSTANT = 100
    private var permissionStatus: SharedPreferences? = null
    private var sentToSettings = false

    internal var permissionsRequired = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    lateinit var imageEncoded: String
    internal var imagesEncodedList: MutableList<String>? = null
    lateinit var fieldsRequired : List<View>

    protected var bitmapImage: Bitmap? = null
    protected var byteImage: ByteArray? = null

    lateinit internal var photoUri: Uri
    var imageFileName: String = ""

    private val onNewMessage = object : Emitter.Listener {
        override fun call(vararg args: Any) {
            this@MainActivity!!.runOnUiThread(Runnable {

                val data = args[0] as JSONArray

                


            })
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        permissionStatus = PreferenceManager
            .getDefaultSharedPreferences(this)

        launchActivity()

        mSocket = IO.socket("https://sleepy-meadow-73150.herokuapp.com")
        mSocket.connect();

        mSocket.on( "updateScreenList" , onNewMessage )

    }

    fun join( view : View ){

        var jsonObject = JSONObject()
        jsonObject.put( "name" , "Admin" )
        jsonObject.put( "room" , "TestRoom" )
        mSocket.emit( "join" , jsonObject )

    }

    fun sendMessage( view : View ){

        val encodedImage = Base64.encodeToString(byteImage, Base64.DEFAULT)

        var jsonObject = JSONObject()
        jsonObject.put( "image" , encodedImage )
        mSocket.emit( "createImage" , jsonObject )
//
//        editText.setText("")

    }

    fun uploadImage ( view: View ){
        openGallery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_GALLERY_PICK){

            if (data!!.data != null) {
                photoUri = data.data
                imageFileName = uri2filename(photoUri);
                setPic(documentImageView, photoUri)
            }

            else {
                if (data.clipData != null) {
                    val mClipData = data.clipData
                    val mArrayUri = ArrayList<Uri>()
                    for (i in 0 until mClipData!!.itemCount) {

                        val item = mClipData.getItemAt(i)
                        val uri = item.uri
                        mArrayUri.add(uri)
                        // Get the cursor
                        val cursor = this.contentResolver.query(uri, filePathColumn, null, null, null)
                        // Move to first row
                        cursor!!.moveToFirst()

                        val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                        imageEncoded = cursor.getString(columnIndex)
                        imagesEncodedList!!.add(imageEncoded)
                        cursor.close()
                    }

                    photoUri = Uri.parse(imagesEncodedList!![0])
                    imageFileName = uri2filename(photoUri);

                    setPic(documentImageView, photoUri)
                    // Log.v("LOG_TAG", "Selected Images" + mArrayUri.size());
                } else {
                    // showLongBaseToast("issue with image caputring contact us at support@op-gamers.com");
                }
            }
        }
    }


    private fun launchActivity() {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                permissionsRequired[0]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, permissionsRequired[0])
            ) {
                //Show Information about why you need the permission
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Need Multiple Permissions")
                builder.setMessage("This app needs Location permissions and Camera permission.")
                builder.setPositiveButton("Grant") { dialog, which ->
                    dialog.cancel()
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        permissionsRequired,
                        PERMISSION_CALLBACK_CONSTANT
                    )
                }
                builder.setNegativeButton("Cancel") { dialog, which ->
                    finish()
                    dialog.cancel()
                }
                builder.show()
            } else if (permissionStatus!!.getBoolean(permissionsRequired[0], false)) {
                //Previously Permission Request was cancelled with 'Dont Ask Again',
                // Redirect to Settings after showing Information about why you need the permission
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Need Multiple Permissions")
                builder.setMessage("This app needs Location permissions.")
                builder.setPositiveButton("Grant") { dialog, which ->
                    dialog.cancel()
                    sentToSettings = true
                    // Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    // Uri uri = Uri.fromParts("package", getPackageName(), null);
                    // intent.setData(uri);
                    //startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                    Toast.makeText(baseContext, "Go to Permissions to Location", Toast.LENGTH_SHORT)
                        .show()
                }
                builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
                builder.show()
            } else {
                //just request the permission
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    permissionsRequired,
                    PERMISSION_CALLBACK_CONSTANT
                )
            }


            val editor = permissionStatus!!.edit()
            editor.putBoolean(permissionsRequired[0], true)
            editor.commit()
        } else {
            //You already have the permission, just go ahead.

        }
    }



    fun openGallery( ) {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        // photoPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        photoPickerIntent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(photoPickerIntent, REQUEST_GALLERY_PICK)
    }

    protected fun setPic(mProfilePhotoIv: ImageView?, parse: Uri) {
        val targetW: Int
        val targetH: Int
        if (mProfilePhotoIv == null) {
            // Get the dimensions of the View
            targetW = 300
            targetH = 300
        } else {
            targetW = mProfilePhotoIv.width
            targetH = mProfilePhotoIv.height
        }

        var inStream: InputStream? = null
        try {
            inStream = this.contentResolver.openInputStream(parse)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inStream, null, bmOptions)

        // Get the dimensions of the bitmap
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight

        // Determine how much to scale down the image
        val scaleFactor = Math.min(photoW / targetW, photoH / targetH)

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inPurgeable = true

        var inpStream: InputStream? = null
        try {
            inpStream = this.contentResolver.openInputStream(parse)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        bitmapImage = BitmapFactory.decodeStream(inpStream, null, bmOptions)
        mProfilePhotoIv!!.setImageBitmap(bitmapImage)
        compressImage(bitmapImage)
    }

    fun compressImage(bitmapImage: Bitmap?) {
        val stream = ByteArrayOutputStream()
        bitmapImage!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        byteImage = stream.toByteArray()

    }

    fun uri2filename(uri: Uri): String {

        var ret: String = ""
        var scheme: String = uri.getScheme();

        if (scheme.equals("file")) {
            ret = uri.getLastPathSegment();
        } else if (scheme.equals("content")) {
            var cursor = this.getContentResolver()?.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                ret = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        }
        return ret;
    }


}
