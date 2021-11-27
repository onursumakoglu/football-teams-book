package com.onursumakoglu.footballteamsbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.onursumakoglu.footballteamsbook.databinding.ActivityTeamDetailsBinding
import android.graphics.Bitmap;
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.widget.Toast
import java.io.ByteArrayOutputStream


class TeamDetailsActivity : AppCompatActivity() {

    private lateinit var binding : ActivityTeamDetailsBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>  // bcs of end of the process we will go to another activity
    private lateinit var permissionLauncher: ActivityResultLauncher<String> // bcs of permisson type is string
    private var selectedBitmap : Bitmap? = null // to convert imageData which returning uri to bitmap -- ln 70
    //private lateinit var selectedBitmap : Bitmap
    private lateinit var database : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeamDetailsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Teams", MODE_PRIVATE, null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info.equals("new")){
            binding.teamNameText.setText("")
            binding.teamYearText.setText("")
            binding.saveButton.visibility = View.VISIBLE
            binding.imageView.setImageResource(R.drawable.select_image)
        }else {
            binding.saveButton.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id", 1)

            val cursor = database.rawQuery("SELECT * FROM teams WHERE id = ?", arrayOf(selectedId.toString()))
            val teamNameIx = cursor.getColumnIndex("teamname")
            val teamYearIx = cursor.getColumnIndex("teamyear")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.teamNameText.setText(cursor.getString(teamNameIx))
                binding.teamYearText.setText(cursor.getString(teamYearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()

        }

    }

    fun saveClicked(view: View){

        val teamName = binding.teamNameText.text.toString()
        val yearText = binding.teamYearText.text.toString()

        if (selectedBitmap != null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!, 300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                database.execSQL("CREATE TABLE IF NOT EXISTS teams (id INTEGER PRIMARY KEY, teamname VARCHAR, teamyear VARCHAR, image BLOB)")

                val sqlString = "INSERT INTO teams (teamname, teamyear, image) VALUES (?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1, teamName)
                statement.bindString(2, yearText)
                statement.bindBlob(3, byteArray)
                statement.execute()

            }catch (e: Exception){
                e.printStackTrace()
            }

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

        }

    }

    private fun makeSmallerBitmap(image: Bitmap, maximumSize: Int) : Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if(bitmapRatio > 1){
            //landscape image
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()

        }else {
            //portrait image
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }

        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    fun selectImage(view: View){

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission
                .READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                // bcs of to show why the user should give permission. for now it is not necessary.
                Snackbar.make(view, "Permission needed for access to gallery", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give Permission", View.OnClickListener {
                        // request permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()

            }else {
                // request permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        }else{
            // permission already given. so user can access to gallery.
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }

    }

    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ resulttt ->
            if (resulttt.resultCode == RESULT_OK){
                val intentFromResult = resulttt.data // nullable döndüğü için aşağıda kontrol ediyoruz. yani boşta dönebilir.
                if (intentFromResult != null){
                    val imageData = intentFromResult.data // returning uri
                    //binding.imageView.setImageURI(imageData)  yukarıdaki veriyi alıp bitmape çevirip küçültmek lazım. sqlite'a kaydetmek için.
                    if (imageData != null){
                        try {
                            if (Build.VERSION.SDK_INT >= 28){ // bcs of imagedecoder func. required min. sdk 28
                                val source = ImageDecoder.createSource(this.contentResolver, imageData)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }else{
                                selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }
                        }catch (e: Exception){
                            e.printStackTrace()
                        }
                    }

                }
            }

        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if (result) {
                //permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }else{
                //permission denied
                Toast.makeText(this, "Permission needed!", Toast.LENGTH_LONG).show()
            }
        }


    }

}