package com.howlab.howlstagram.util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.howlab.howlstagram.model.PushModel
import com.squareup.okhttp.*
import java.io.IOException

class FcmManager {

    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "key=AAAAjuVUl_I:APA91bGRhU3TOPL-925aVbHe30Jhl8yWwKXlryvH0lt_WmDwk27Qm4ETQJCetK0vVB64H66phipMYLGUXCRHrd6UWVrpEmQW3GsKPfDBdUtKuvhBEvlDsG3EPgPFF3yJP8tTN43a4gmI"

    var okHttpClient : OkHttpClient? = null
    var gson : Gson? = null
    lateinit var firestore : FirebaseFirestore

    companion object{
        var instance = FcmManager()

    }

    init {
        firestore = FirebaseFirestore.getInstance()
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(dUid : String, title : String, message : String){

        firestore.collection("pushtokens").document(dUid).get().addOnCompleteListener {
            result ->

            var token = result.result["token"].toString()

            var pushModel = PushModel()
            pushModel.to = token
            pushModel.notification.title = title
            pushModel.notification.body = message

            var json = gson?.toJson(pushModel)

            var body = RequestBody.create(JSON,json)


            var request = Request.Builder()
                .addHeader("Content-Type","application/json")
                .addHeader("Authorization",serverKey)
                .url(url)
                .post(body)
                .build()


            okHttpClient?.newCall(request)?.enqueue(object : Callback{
                override fun onFailure(request: Request?, e: IOException?) {

                }

                override fun onResponse(response: Response?) {
                    println(response?.body()?.string())
                }

            })
        }


    }
}