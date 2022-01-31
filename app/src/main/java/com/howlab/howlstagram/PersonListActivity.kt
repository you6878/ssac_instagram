package com.howlab.howlstagram

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.howlab.howlstagram.databinding.ActivityPersonListBinding
import com.howlab.howlstagram.databinding.ItemPersonBinding
import com.howlab.howlstagram.model.FollowModel

class PersonListActivity : AppCompatActivity() {
    lateinit var binding : ActivityPersonListBinding
    var following = false
    lateinit var followModel : FollowModel
    lateinit var ids : Array<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        following = intent.getBooleanExtra("Mode",false);
        followModel = intent.getParcelableExtra("FM")!!
        binding = DataBindingUtil.setContentView(this,R.layout.activity_person_list)
        binding.personRecyclerview.adapter = PersonListAdapter()
        binding.personRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.toolbarBtnBack.setOnClickListener {
            finish()
        }
        if(following){
            //팔로잉일때
            ids =  followModel.followings.values.toTypedArray()
        }else{
            ids =  followModel.followers.values.toTypedArray()
        }
    }

    inner class ItemPersonViewHolder(var binding : ItemPersonBinding) : RecyclerView.ViewHolder(binding.root)
    inner class PersonListAdapter : RecyclerView.Adapter<ItemPersonViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPersonViewHolder {
            var view = ItemPersonBinding.inflate(LayoutInflater.from(parent.context),parent,false)
            return ItemPersonViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemPersonViewHolder, position: Int) {
            holder.binding.profileTextview.text = ids[position]
            holder.binding.messageTextview.visibility = View.INVISIBLE
        }

        override fun getItemCount(): Int {
            return ids.size
        }

    }
}