package com.howlab.howlstagram.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.howlab.howlstagram.LoginActivity
import com.howlab.howlstagram.MainActivity
import com.howlab.howlstagram.PersonListActivity
import com.howlab.howlstagram.R
import com.howlab.howlstagram.databinding.FragmentDetailViewBinding
import com.howlab.howlstagram.databinding.FragmentUserBinding
import com.howlab.howlstagram.databinding.ItemImageviewBinding
import com.howlab.howlstagram.model.AlarmModel
import com.howlab.howlstagram.model.ContentModel
import com.howlab.howlstagram.model.FollowModel
import com.howlab.howlstagram.util.FcmManager

class UserFragment : Fragment() {
    lateinit var binding : FragmentUserBinding
    lateinit var firestore: FirebaseFirestore
    var dUid : String? = null
    var userId : String? = null
    var currentUid : String? = null
    lateinit var auth : FirebaseAuth
    lateinit var storeage : FirebaseStorage
    var followModel = FollowModel()
    var myPhotoResultLanucher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        //사진을 받아오며 바로 업로드 해주는 부분
        var imageUrl = result.data!!.data
        //사진을 저장할 경로
        var storageRef = storeage.reference.child("userProfileImages").child(currentUid!!)
        storageRef.putFile(imageUrl!!).continueWithTask {
            //스토리지에 사진만 업로드
            return@continueWithTask storageRef.downloadUrl
        }.addOnCompleteListener {
            imageUri ->
            //데이터베이스 누가 뭘 올렸는지 정리한 데이터 저장
            var map = HashMap<String,Any>()
            map["image"] = imageUri.result.toString()

            firestore.collection("profileImages").document(currentUid!!).set(map)
        }

    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firestore = FirebaseFirestore.getInstance()
        storeage = FirebaseStorage.getInstance()
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user,container,false)
        currentUid = FirebaseAuth.getInstance().uid // 나의 UID
        auth = FirebaseAuth.getInstance()
        dUid = arguments?.getString("dUid")
        userId = arguments?.getString("userId")

        binding.accountRecyclerview.adapter = UserFragmentRecyclerviewAdapter()
        binding.accountRecyclerview.layoutManager = GridLayoutManager(activity,3)

        var mainActivity = activity as? MainActivity

        if(currentUid == dUid){
            //나의페이지
            mainActivity?.binding?.toolbarLogo?.visibility = View.VISIBLE
            mainActivity?.binding?.toolbarUsername?.visibility = View.INVISIBLE
            mainActivity?.binding?.toolbarBtnBack?.visibility = View.INVISIBLE
            binding.accountBtnFollowSignout.text = activity?.getText(R.string.signout)
            binding.accountBtnFollowSignout.setOnClickListener {
                auth.signOut()
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
            }
            binding.accountIvProfile.setOnClickListener {
                var picker = Intent(Intent.ACTION_PICK)
                picker.type = "image/*"
                myPhotoResultLanucher.launch(picker)
            }


        }else{
            //상대방 페이지
            mainActivity?.binding?.toolbarLogo?.visibility = View.INVISIBLE
            mainActivity?.binding?.toolbarUsername?.visibility = View.VISIBLE
            mainActivity?.binding?.toolbarBtnBack?.visibility = View.VISIBLE

            mainActivity?.binding?.toolbarUsername?.text = userId
            mainActivity?.binding?.toolbarBtnBack?.setOnClickListener {
                mainActivity?.binding?.bottomNavigation.selectedItemId = R.id.action_home
            }
            binding.accountBtnFollowSignout.text = activity?.getText(R.string.follow)

            binding.accountBtnFollowSignout.setOnClickListener {
                reqeustFollwerAndFollowing()
            }
        }
        getProfileImage()
        getFollowingFollowingCount()

        binding.followerLinearlayout.setOnClickListener {
            var i = Intent(activity,PersonListActivity::class.java)
            i.putExtra("FM",followModel)
            i.putExtra("Mode", false)
            startActivity(i)
        }
        binding.followingLinearlayout.setOnClickListener {
            var i = Intent(activity,PersonListActivity::class.java)
            i.putExtra("FM",followModel)
            i.putExtra("Mode", true)
            startActivity(i)
        }
        return binding.root
    }
    fun getFollowingFollowingCount(){
        firestore.collection("users").document(dUid!!).addSnapshotListener { value, error ->
            if(value == null) return@addSnapshotListener
            followModel = value.toObject(FollowModel::class.java)!!
            if(followModel?.followerCount != null){
                //연예인이 스토커들을 관리하는 부분
                binding.accountFollowerTextview.text = followModel?.followerCount.toString()
                if(currentUid == dUid){
                    //나의 페이지 일경우
                    return@addSnapshotListener
                }
                //상대페이지
                if(followModel.followers.containsKey(currentUid)){
                    binding.accountBtnFollowSignout.text = activity?.getText(R.string.follow_cancel)
                }else{
                    binding.accountBtnFollowSignout.text = activity?.getText(R.string.follow)
                }
            }

            if(followModel?.followingCount != null){
                //스토커가 연예인들 카운트
                binding.accountFollowingTextview.text = followModel.followingCount.toString()
            }

        }
    }
    fun eventFollowAlarm(dUid : String){
        var alarmModel = AlarmModel()
        alarmModel.destinationUid = dUid
        alarmModel.userId = auth.currentUser?.email
        alarmModel.uid = auth.uid
        alarmModel.kind = 2
        alarmModel.timestamp = System.currentTimeMillis()

        firestore.collection("alarms").document().set(alarmModel)
        var message = auth.currentUser?.email + "이 나를 팔로우 하기 시작했습니다."

        FcmManager.instance.sendMessage(dUid,"Howlstagram",message)
    }


    fun reqeustFollwerAndFollowing(){
        //스토커가 누구를 쫒아다니는지 정리한 메
        var tsDocFollowing = firestore.collection("users").document(currentUid!!)

        firestore.runTransaction { transition ->
            var followingModel = transition.get(tsDocFollowing).toObject(FollowModel::class.java)
            if(followingModel == null){
                //스토커가 순수한 상태 한명도 쫒아다니지 않음
                followingModel = FollowModel()
                followingModel.followingCount = 1
                followingModel.followings[dUid!!] = userId!!
                transition.set(tsDocFollowing,followingModel)
                return@runTransaction
            }else if(followingModel.followings.containsKey(dUid)){
                //스토커가 더이상이 포기
                followingModel.followingCount = followingModel.followingCount - 1
                followingModel.followings.remove(dUid)
            }else{
                //스토커가 한명만 쫒아다닌게 아닌 멀티
                followingModel.followingCount = followingModel.followingCount + 1
                followingModel.followings[dUid!!] = userId!!
            }
            transition.set(tsDocFollowing,followingModel)
            return@runTransaction

        }

        //연예인이 누구에게 스토킹을 당하는지 (현재 로그인된 UID -> 선택한 사람)
        var tsDocFollower = firestore.collection("users").document(dUid!!)
        var sId = auth.currentUser?.email
        firestore?.runTransaction {
            transition ->
            var followModel = transition.get(tsDocFollower!!).toObject(FollowModel::class.java)

            if(followModel == null){
                //아무도 스토킹을 당하지 않을경우(첫 스X킹)
                followModel = FollowModel()
                followModel.followerCount = 1
                followModel.followers[currentUid!!] = sId!!
                eventFollowAlarm(dUid!!)
                transition.set(tsDocFollower,followModel)
                return@runTransaction
            }else if(followModel.followers.containsKey(currentUid)){
                //로그인된 아이디가 누구를 스토킹을 이미 했을때
                followModel.followerCount = followModel.followerCount - 1
                followModel.followers.remove(currentUid)

            }else{
                //아직 아무도 스토킹을 하지 않았을때(인기가 있어서 상대방이 쫒아다닐때)
                followModel.followerCount = followModel.followerCount + 1
                followModel.followers[currentUid!!] = sId!!
                eventFollowAlarm(dUid!!)
            }
            transition.set(tsDocFollower,followModel)
            return@runTransaction
        }


    }
    fun getProfileImage(){
        firestore.collection("profileImages").document(dUid!!).addSnapshotListener { value, error ->
            if(value?.data != null){
                var url = value.data!!["image"]
                Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop()).into(binding.accountIvProfile)
            }
        }
    }
    inner class CellImageViewHolder(val binding : ItemImageviewBinding) : RecyclerView.ViewHolder(binding.root)
    inner class UserFragmentRecyclerviewAdapter : RecyclerView.Adapter<CellImageViewHolder>(){
        var contentModels : ArrayList<ContentModel> = arrayListOf()
        init {
            firestore.collection("images").whereEqualTo("uid",dUid).addSnapshotListener { value, error ->

                for(item in value!!.documentChanges){
                    if(item.type == DocumentChange.Type.ADDED){
                        contentModels.add(item.document.toObject(ContentModel::class.java)!!)
                    }
                }
                binding.accountPostTextview.text = contentModels.size.toString()

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
        }

        override fun getItemCount(): Int {
            return contentModels.size
        }

    }
}

