package com.howlab.howlstagram

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.howlab.howlstagram.databinding.ActivityMainBinding
import com.howlab.howlstagram.fragment.AlarmFragment
import com.howlab.howlstagram.fragment.DetailViewFragment
import com.howlab.howlstagram.fragment.GridFragment
import com.howlab.howlstagram.fragment.UserFragment
import com.howlab.howlstagram.util.FcmManager

class MainActivity : AppCompatActivity(),BottomNavigationView.OnNavigationItemSelectedListener {
    lateinit var binding : ActivityMainBinding
    lateinit var auth : FirebaseAuth
    lateinit var message : FirebaseMessaging
    lateinit var firestore : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        auth = FirebaseAuth.getInstance()
        message = FirebaseMessaging.getInstance()
        firestore = FirebaseFirestore.getInstance()
        binding.bottomNavigation.setOnNavigationItemSelectedListener(this)
        binding.bottomNavigation.selectedItemId = R.id.action_home
        binding.bottomNavigation.findViewById<View>(R.id.action_add_photo).setOnLongClickListener {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                var i = Intent(this, AddPhotoActivity::class.java)
                i.putExtra("isBeauty", true)
                startActivity(i)
            }
            return@setOnLongClickListener true
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA),0)
        saveMyPushToken()

    }
    fun saveMyPushToken(){
        message.token.addOnCompleteListener { task ->
            if(task.isSuccessful){

                var token = task.result
                var map = mutableMapOf<String,Any>()
                map["token"] = token

                firestore.collection("pushtokens").document(auth?.uid!!).set(map)

            }

        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
      when(item.itemId){
          R.id.action_home->{

              binding.toolbarLogo.visibility = View.VISIBLE
              binding.toolbarUsername.visibility = View.INVISIBLE
              binding.toolbarBtnBack.visibility = View.INVISIBLE

              var f = DetailViewFragment()
              supportFragmentManager.beginTransaction().replace(R.id.main_content,f).commit()

              return true
          }
          R.id.action_search->{
              var f = GridFragment()
              supportFragmentManager.beginTransaction().replace(R.id.main_content,f).commit()
              return true
          }
          R.id.action_add_photo ->{
              if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                  //사진 읽기(READ_EXTERNAL_STORAGE) 있을때
                startActivity(Intent(this,AddPhotoActivity::class.java))

              }else{
                  //사진 읽기(READ_EXTERNAL_STORAGE) 없을때
                  Toast.makeText(this,getString(R.string.donothave_permission),Toast.LENGTH_LONG).show()
              }


              return true
          }
          R.id.action_favorite_alarm->{
              var f = AlarmFragment()
              supportFragmentManager.beginTransaction().replace(R.id.main_content,f).commit()
              return true
          }

          R.id.action_account->{
              var f = UserFragment()
              var b = Bundle()
              b.putString("dUid",auth.uid)
              f.arguments = b
              supportFragmentManager.beginTransaction().replace(R.id.main_content,f).commit()
              return true
          }

      }
        return false
    }
}