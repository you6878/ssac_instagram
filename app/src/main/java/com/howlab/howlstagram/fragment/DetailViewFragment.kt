package com.howlab.howlstagram.fragment

import android.content.Intent
import android.os.Bundle
import android.view.ContentInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.howlab.howlstagram.CommentActivity
import com.howlab.howlstagram.R
import com.howlab.howlstagram.databinding.FragmentDetailViewBinding
import com.howlab.howlstagram.databinding.ItemDetailBinding
import com.howlab.howlstagram.model.AlarmModel
import com.howlab.howlstagram.model.ContentModel
import com.howlab.howlstagram.model.FollowModel

class DetailViewFragment : Fragment() {
    lateinit var binding : FragmentDetailViewBinding
    lateinit var firestore : FirebaseFirestore
    lateinit var uid : String
    lateinit var auth : FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_detail_view,container,false)
        uid = FirebaseAuth.getInstance().uid!!
        firestore.collection("users").document(uid).get().addOnSuccessListener {
            result ->
            var followModel = result.toObject(FollowModel::class.java)
            if(followModel?.followings?.keys != null && followModel.followings.keys.size > 0){

                binding.detailviewRecyclerveiw.adapter = DetailviewRecyclerviewAdapter(followModel?.followings.keys.toList())
                binding.detailviewRecyclerveiw.layoutManager = LinearLayoutManager(activity)
            }

        }

        return binding.root
    }
    inner class DetailViewHolder(var binding : ItemDetailBinding) : RecyclerView.ViewHolder(binding.root)
    inner class DetailviewRecyclerviewAdapter(toList : List<String>) : RecyclerView.Adapter<DetailViewHolder>(){

        var contentModels = arrayListOf<ContentModel>()
        var contentUidsList = arrayListOf<String>()
        init {
            //???????????? ?????? ???????????? ?????? Snapshot -> ???????????? ?????? ?????? -> ?????? ?????? ?????? -> ??????????????????
            //????????? ???????????? ?????? ??????????????? Get

            firestore.collection("images")
                .whereIn("uid",toList)
                .addSnapshotListener { value, error ->
                for (item in value!!.documentChanges){
                    if(item.type == DocumentChange.Type.ADDED){
                        var contentModel = item.document.toObject(ContentModel::class.java)
                        contentModels.add(contentModel!!)
                        contentUidsList.add(item.document.id)
                    }

                }
                notifyDataSetChanged()
            }
            //???????????? ???????????? ????????? ??????????????? ?????????

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
            //??? ????????? ?????? ???????????? XML ????????? ???????????? ?????? ?????? ?????? ????????????.
            var view = ItemDetailBinding.inflate(LayoutInflater.from(parent.context),parent,false)

            return DetailViewHolder(view)
        }

        override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
            //???????????? ?????????
            var contentModel = contentModels[position]
            var viewHolder = holder.binding
            viewHolder.profileTextview.text = contentModel.userId
            viewHolder.likeTextview.text = "Likes " + contentModel.favoriteCount
            viewHolder.explainTextview.text = contentModel.explain
            viewHolder.favoriteImageview.setOnClickListener {
                eventFavorite(position)
            }
            //????????? ??????????????? ??????
            viewHolder.profileTextview.setOnClickListener {
                var userFragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("dUid",contentModel.uid)
                bundle.putString("userId",contentModel.userId)
                userFragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,userFragment)?.commit()

            }
            if(contentModel.favorites.containsKey(uid)){
                //?????? ???????????? ????????????
                viewHolder.favoriteImageview.setImageResource(R.drawable.ic_favorite)
            }else{
                viewHolder.favoriteImageview.setImageResource(R.drawable.ic_favorite_border)
            }
            Glide.with(holder.itemView.context).load(contentModel.imageUrl).into(viewHolder.contentImageview)

            //????????? ???????????? ??????
            viewHolder.commentImageview.setOnClickListener {
                var i = Intent(activity,CommentActivity::class.java)
                i.putExtra("dUid",contentUidsList[position])
                startActivity(i)
            }

        }
        fun favorteAlarm(dUid : String){
            var alarmModel = AlarmModel()
            alarmModel.destinationUid = dUid
            alarmModel.userId = auth.currentUser?.email
            alarmModel.uid = auth.uid
            alarmModel.kind = 0
            alarmModel.timestamp = System.currentTimeMillis()

        }

        override fun getItemCount(): Int {

            return contentModels.size
        }
        fun eventFavorite(position : Int){
            var docId = contentUidsList[position]
            var tsDoc = firestore.collection("images").document(docId)
            firestore.runTransaction {
                transition ->
                var contentDTO = transition.get(tsDoc).toObject(ContentModel::class.java)
                if(contentDTO!!.favorites.containsKey(uid)){
                    //????????? ?????? ??????
                    contentDTO.favoriteCount = contentDTO.favoriteCount - 1
                    contentDTO.favorites.remove(uid)

                }else{
                    //???????????? ????????? ?????? ??????
                    contentDTO.favoriteCount = contentDTO.favoriteCount + 1
                    contentDTO.favorites[uid] = true
                    favorteAlarm(contentDTO.uid!!)

                }
                transition.set(tsDoc,contentDTO)
            }

        }

    }
}