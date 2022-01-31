package com.howlab.howlstagram

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.FirebaseFirestore

class ImageCache {
    var userBitmap = hashMapOf<String, Bitmap>()
    companion object{
        var instance = ImageCache()
    }

    fun getBitmap(context : Context, dUid : String, imageview : ImageView){

        if(userBitmap[dUid] != null){
            //이미지를 한번이라도 불러왔을 경우
            imageview.setImageBitmap(userBitmap[dUid])
        }else{
            //불러오지 않았을 경우
            FirebaseFirestore.getInstance().collection("profileImages").document(dUid).get().addOnCompleteListener {
                res ->

                var url = res.result["image"].toString()
                Glide.with(context).asBitmap().load(url).apply(RequestOptions().circleCrop()).into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        userBitmap[dUid] = resource
                        imageview.setImageBitmap(userBitmap[dUid])
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }

                })
            }
        }

    }
}