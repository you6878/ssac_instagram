package com.howlab.howlstagram

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.howlab.howlstagram.beauty.FaceDectionActivity
import com.howlab.howlstagram.databinding.ActivityAddPhotoBinding
import com.howlab.howlstagram.model.ContentModel
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    lateinit var binding : ActivityAddPhotoBinding
    lateinit var storage : FirebaseStorage
    lateinit var auth : FirebaseAuth
    lateinit var firestore : FirebaseFirestore
    var photoUri : Uri? = null

    var isBeauty = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_add_photo)
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        isBeauty = intent.getBooleanExtra("isBeauty",false)

        if(isBeauty){
            //FaceDectionActivity
            var i = Intent(this,FaceDectionActivity::class.java)
            beautyResult.launch(i)

        }else{
            //앨범을 띄워주면 됩니다.
            var i = Intent(Intent.ACTION_PICK)
            i.type = "image/*"
            photoResult.launch(i)
        }
        binding.addphotoUploadBtn.setOnClickListener {
            contentUpload()
        }

    }
    fun contentUpload(){
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "IMAGE_" + timestamp + ".png"


        var storagePath = storage.reference?.child("images").child(imageFileName)

        storagePath.putFile(photoUri!!).continueWithTask {
            return@continueWithTask storagePath.downloadUrl
        }.addOnCompleteListener {
            downloadUrl ->

            var contentModel = ContentModel()

            contentModel.imageUrl = downloadUrl.result.toString()
            contentModel.explain = binding.addphotoEditEdittext.text.toString()
            contentModel.uid = auth?.uid
            contentModel.userId = auth?.currentUser?.email
            contentModel.timestamp = System.currentTimeMillis()

            firestore.collection("images").document().set(contentModel)

            Toast.makeText(this,"업로드에 성공하였습니다.",Toast.LENGTH_LONG).show()

            finish()

        }

    }
    fun getImageUri(context : Context, bitmap : Bitmap): Uri? {
        var bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,bytes)
        var path = MediaStore.Images.Media.insertImage(context.contentResolver,bitmap,"Title",null)
        return Uri.parse(path)

    }
    var photoResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        //사진 받는 부분
        photoUri = result.data?.data
        binding.uploadImageview.setImageURI(photoUri)
    }
    var beautyResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
        //사진 받는 부분
//        photoUri = result.data?.data
//        binding.uploadImageview.setImageURI(photoUri)
        var bitmap = result.data?.getParcelableExtra<Bitmap>("Bitmap")
        binding.uploadImageview.setImageBitmap(bitmap)
        photoUri = getImageUri(this,bitmap!!)
    }

}