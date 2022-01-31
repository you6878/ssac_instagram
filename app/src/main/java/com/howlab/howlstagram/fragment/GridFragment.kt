package com.howlab.howlstagram.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.howlab.howlstagram.R
import com.howlab.howlstagram.databinding.FragmentDetailViewBinding
import com.howlab.howlstagram.databinding.FragmentGridBinding
import com.howlab.howlstagram.databinding.ItemImageviewBinding
import com.howlab.howlstagram.model.ContentModel

class GridFragment : Fragment() {
    lateinit var binding : FragmentGridBinding
    lateinit var firestore : FirebaseFirestore
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_grid,container,false)
        firestore = FirebaseFirestore.getInstance()
        binding.gridRecyclerview.adapter = GridFragmentRecyclerviewAdapter()
        binding.gridRecyclerview.layoutManager = GridLayoutManager(activity,3)
        return binding.root

    }
    inner class CellImageViewHolder(val binding : ItemImageviewBinding) : RecyclerView.ViewHolder(binding.root)
    inner class GridFragmentRecyclerviewAdapter : RecyclerView.Adapter<CellImageViewHolder>(){
        var contentModels : ArrayList<ContentModel> = arrayListOf()
        init {
            firestore.collection("images").addSnapshotListener { value, error ->

                for(item in value!!.documentChanges){
                    if(item.type == DocumentChange.Type.ADDED){
                        contentModels.add(item.document.toObject(ContentModel::class.java)!!)
                    }
                }

                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellImageViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var view = ItemImageviewBinding.inflate(LayoutInflater.from(parent.context),parent,false)
            view.cellImageview.layoutParams = LinearLayoutCompat.LayoutParams(width,width)
            return CellImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: CellImageViewHolder, position: Int) {
            var contentModel = contentModels[position]
            Glide.with(holder.itemView.context).load(contentModel.imageUrl).into(holder.binding.cellImageview)

            //상대방 유저페이지 이동
            holder.binding.cellImageview.setOnClickListener {
                var userFragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("dUid",contentModel.uid)
                bundle.putString("userId",contentModel.userId)
                userFragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,userFragment)?.commit()

            }
        }

        override fun getItemCount(): Int {
            return contentModels.size
        }

    }
}