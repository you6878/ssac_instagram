package com.howlab.howlstagram

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.howlab.howlstagram.databinding.ActivityCommentBinding
import com.howlab.howlstagram.databinding.ItemPersonBinding
import com.howlab.howlstagram.model.AlarmModel
import com.howlab.howlstagram.model.ContentModel
import com.howlab.howlstagram.util.FcmManager

class CommentActivity : AppCompatActivity() {
    lateinit var binding: ActivityCommentBinding
    var contentUid: String? = null
    var dUid: String? = null
    lateinit var auth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        contentUid = intent.getStringExtra("ContentUid") //ContentUid
        dUid = intent.getStringExtra("dUid") //ContentUid
        //사람의 UID
        binding = DataBindingUtil.setContentView(this, R.layout.activity_comment)
        binding.commentRecyclerview.adapter = CommentAdapter()
        binding.commentRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.sendBtn.setOnClickListener {
            var comment = ContentModel.Comment()
            comment.uid = auth.currentUser?.uid
            comment.userId = auth.currentUser?.email
            comment.comment = binding.commentEdittext.text.toString()
            comment.timestamp = System.currentTimeMillis()
            firestore.collection("images").document(contentUid!!)?.collection("comments").document()
                .set(comment)
            commentAlarm(dUid!!,binding.commentEdittext.text.toString())
            binding.commentEdittext.setText("")
        }
    }
    fun commentAlarm(dUid : String, message : String){
        var alarmModel = AlarmModel()
        alarmModel.destinationUid = dUid
        alarmModel.uid = auth.uid
        alarmModel.userId = auth.currentUser?.email
        alarmModel.kind = 1
        alarmModel.message = message
        alarmModel.timestamp = System.currentTimeMillis()

        firestore.collection("alarms").document().set(alarmModel)

        var str = auth.currentUser?.email + "가 " +alarmModel.message +"라는 메세지를 남겼습니다."
        FcmManager.instance.sendMessage(dUid,"Howlstagram",str)
    }

    inner class ItemPersonViewHolder(var binding: ItemPersonBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class CommentAdapter : RecyclerView.Adapter<ItemPersonViewHolder>() {
        var comments = arrayListOf<ContentModel.Comment>()

        init {
            firestore.collection("images").document(contentUid!!).collection("comments")
                .addSnapshotListener { value, error ->
                    comments.clear()

                    if (value == null) return@addSnapshotListener

                    for (item in value.documents) {
                        comments.add(item.toObject(ContentModel.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPersonViewHolder {
            var view = ItemPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ItemPersonViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemPersonViewHolder, position: Int) {
                var view = holder.binding
            view.profileTextview.text = comments[position].userId
            view.messageTextview.text = comments[position].comment
            ImageCache.instance.getBitmap(this@CommentActivity,comments[position].uid!!,view.profileImageview)
        }

        override fun getItemCount(): Int {
            return comments.size
        }

    }
}