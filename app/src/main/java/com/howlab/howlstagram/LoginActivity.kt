package com.howlab.howlstagram

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.howlab.howlstagram.databinding.ActivityLoginBinding
import android.content.pm.PackageManager

import android.content.pm.PackageInfo
import android.util.Base64
import android.util.Log
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import java.lang.Exception
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {

    lateinit var auth : FirebaseAuth
    lateinit var binding : ActivityLoginBinding
    lateinit var googleSigninClient : GoogleSignInClient
    var TAG = "LoginActivity"
    lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        binding.emailLoginButton.setOnClickListener {
            signinAndSignup()
        }
        binding.findIdPasswordButton.setOnClickListener {
            startActivity(Intent(this,FindIdActivity::class.java))
        }
        //GoogleLogin
        binding.googleLoginButton.setOnClickListener {
            googleLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSigninClient = GoogleSignIn.getClient(this,gso)

        //Facebook
        callbackManager = CallbackManager.Factory.create()
        binding.facebookLoginButton.setOnClickListener {
            facebookLogin()
        }
        moveMain(auth.currentUser)
//        printHashKey(this)
    }
    fun googleLogin(){
        var i = googleSigninClient.signInIntent
        googleLoginResult.launch(i)
    }

    fun facebookLogin(){
        var loginManager = LoginManager.getInstance()
        //로그인 요청
        loginManager.logInWithReadPermissions(this, Arrays.asList("public_profile","email"))
        loginManager.registerCallback(callbackManager,object : FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult?) {
                var token = result?.accessToken
                firebaseAuthWithFacebook(token)
            }

            override fun onCancel() {

            }

            override fun onError(error: FacebookException?) {

            }

        })
    }

    fun firebaseAuthWithFacebook(idToken: AccessToken?) {
        var credential = FacebookAuthProvider.getCredential(idToken!!.token)
        auth.signInWithCredential(credential).addOnCompleteListener {
                task ->
            if(task.isSuccessful){
                if(auth.currentUser!!.isEmailVerified){
                    //이메일 인증이 되었을때
                    moveMain(auth.currentUser)
                }else{
                    //이메일 인증이 안됬을때
                    saveFindIdData()
                }

            }
        }
    }

    fun firebaseAuthWithGoogle(idToken: String?) {
        var credential = GoogleAuthProvider.getCredential(idToken,null)
        auth.signInWithCredential(credential).addOnCompleteListener {
                task ->
            if(task.isSuccessful){
                if(auth.currentUser!!.isEmailVerified){
                    //이메일 인증이 되었을때
                    moveMain(auth.currentUser)
                }else{
                    //이메일 인증이 안됬을때
                    saveFindIdData()
                }

            }
        }
    }


    fun signinAndSignup(){
        var id = binding.edittextId.text.toString()
        var password = binding.edittextPassword.text.toString()
        auth.createUserWithEmailAndPassword(id,password).addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                saveFindIdData()
//                moveMain(task.result?.user)
                //아이디 생성 -> 메인화면 이동
            } else {
                //이미 아이디가 있을 경우
                signinEmail()
            }
        }
    }
    fun saveFindIdData(){
        finish()
        startActivity(Intent(this, InputNumberActivity::class.java))
    }
    fun moveMain(user : FirebaseUser?){
        if(user != null){
            if(user.isEmailVerified){
                finish()
                startActivity(Intent(this,MainActivity::class.java))
            }else{
                user.sendEmailVerification()
            }
        }
    }
    fun signinEmail(){
        var id = binding.edittextId.text.toString()
        var password = binding.edittextPassword.text.toString()
        auth.signInWithEmailAndPassword(id,password).addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                moveMain(task.result?.user)
            }
        }
    }
    var googleLoginResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
        // 구글 로그인이 성공했을때 이메일 값이 넘어오는데 -> Token -> Email -> Firebase 서버
        var data = result.data
        var task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.getResult(ApiException::class.java)
        firebaseAuthWithGoogle(account.idToken)
    }
    fun printHashKey(pContext: Context) {
        try {
            val info: PackageInfo = pContext.getPackageManager()
                .getPackageInfo(pContext.getPackageName(), PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey: String = String(Base64.encode(md.digest(), 0))
                Log.i(TAG, "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "printHashKey()", e)
        } catch (e: Exception) {
            Log.e(TAG, "printHashKey()", e)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode,resultCode,data)
    }

}