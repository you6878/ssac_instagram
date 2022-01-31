package com.howlab.howlstagram.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.howlab.howlstagram.R
import com.howlab.howlstagram.databinding.FragmentAlarmBinding
import com.howlab.howlstagram.databinding.FragmentDetailViewBinding
import com.howlab.howlstagram.databinding.ItemPersonBinding
import com.howlab.howlstagram.model.AlarmModel

class AlarmFragment : Fragment() {
    lateinit var binding : FragmentAlarmBinding
    lateinit var auth : FirebaseAuth
    lateinit var firestore : FirebaseFirestore
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm,container,false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        binding.alarmRecyclerview.adapter = AlarmAdapter()
        binding.alarmRecyclerview.layoutManager = LinearLayoutManager(activity)
        return binding.root

    }
    inner class ItemPersonViewHolder(var binding : ItemPersonBinding) : RecyclerView.ViewHolder(binding.root)
    inner class AlarmAdapter : RecyclerView.Adapter<ItemPersonViewHolder>(){
        var alarmList = arrayListOf<AlarmModel>()

        init {
            var uid = auth.uid
            firestore.collection("alarms").whereEqualTo("destinationUid",uid).addSnapshotListener { value, error ->
                alarmList.clear()
                for (item in value!!.documents){
                    alarmList.add(item.toObject(AlarmModel::class.java)!!)
                }
                notifyDataSetChanged()
            }

        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPersonViewHolder {
            var view = ItemPersonBinding.inflate(LayoutInflater.from(parent.context),parent,false)
            return ItemPersonViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemPersonViewHolder, position: Int) {
            var view = holder.binding
            var alarmModel = alarmList[position]
            view.messageTextview.visibility = View.INVISIBLE
            when(alarmModel.kind){
                0 ->{
                    var m = alarmModel.userId + "가 좋아요를 눌렀습니다."
                    view.profileTextview.text = m
                }
                1 ->{
                    var m_1 = alarmModel.userId +"가" + alarmModel.message + "라는 메세지를 남겼습니다."
                    view.profileTextview.text = m_1
                }
                2 ->{
                    var m_2 = alarmModel.userId +"가 나를 팔로우 하기 시작했습니다."
                    view.profileTextview.text = m_2
                }
            }
        }

        override fun getItemCount(): Int {
            return alarmList.size
        }

    }
}