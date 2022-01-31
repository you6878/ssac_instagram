package com.howlab.howlstagram.beauty


import android.content.Intent
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import com.howlab.howlstagram.R
import com.howlab.howlstagram.databinding.ActivityFaceDectionBinding
import com.howlab.howlstagram.util.rotateWithReverse
import com.howlab.howlstagram.util.toBitmap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class FaceDectionActivity : AppCompatActivity() {
    lateinit var binding: ActivityFaceDectionBinding
    //카메라 세팅 해주는 변수
    var camera : Camera? = null
    var cameraProvider : ProcessCameraProvider? = null
    var cameraExecutor : ExecutorService? = null

    // 미리보기 변수
    var preview : Preview? = null
    var w = 320
    var h = 240


    //실시간으로 이미지를 캡쳐하는 부분
    var imageAnalysis : ImageAnalysis? = null
    var lastBitmap : Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_face_dection)
        cameraExecutor = Executors.newSingleThreadExecutor()
        var cpf = ProcessCameraProvider.getInstance(this)
        cpf.addListener({
            cameraProvider = cpf.get()
            var rotation = Surface.ROTATION_270
            var cs = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

            preview = Preview.Builder()
                .setTargetResolution(Size(w,h))
                .setTargetRotation(rotation)
                .build()
            //이미지 캐치
            imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(w,h))
                .setTargetRotation(rotation)
                .build()
            imageAnalysis?.setAnalyzer(cameraExecutor!!,LuminosityAnalyzer())

            cameraProvider?.unbindAll()

            camera = cameraProvider?.bindToLifecycle(this,cs,preview,imageAnalysis)
            preview?.setSurfaceProvider(binding.preview.surfaceProvider)


        },ContextCompat.getMainExecutor(this))
        binding.captureBtn.setOnClickListener {
            var i = Intent()
            i.putExtra("Bitmap",lastBitmap)
            setResult(RESULT_OK,i)
            finish()
        }
    }
    fun makeLens(face : FirebaseVisionFace, faceBitmap : Bitmap){
        var canvas = Canvas(faceBitmap)
        drawLensToLandmark(canvas,face,FirebaseVisionFaceLandmark.LEFT_EYE)
        drawLensToLandmark(canvas,face,FirebaseVisionFaceLandmark.RIGHT_EYE)

    }
    fun drawLensToLandmark(canvas : Canvas?, face : FirebaseVisionFace, landMark : Int){
        var point = face.getLandmark(landMark)?.position

        if(point != null){
            var imageEdgeSize = face.boundingBox.height() / 20f
            var left = 240 - point.x - imageEdgeSize
            var top = point.y - imageEdgeSize
            var right = 240 - point.x + imageEdgeSize
            var bottom = point.y + imageEdgeSize
            canvas?.drawBitmap(BitmapFactory.decodeResource(resources,R.drawable.lens),null, Rect(left.toInt(),top.toInt(),right.toInt(),bottom.toInt()),null)

        }
    }
    inner class LuminosityAnalyzer() : ImageAnalysis.Analyzer{
        var option = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .build()
        override fun analyze(image: ImageProxy) {
            //실시간으로 이미지 넘어오는 부분 1초 20frame 0.05


            var bitmap = image.image!!.toBitmap(100).rotateWithReverse(270f)

            var metaData = FirebaseVisionImageMetadata.Builder()
                .setWidth(image.width)
                .setHeight(image.height)
                .setFormat(IMAGE_FORMAT_NV21)
                .setRotation(FirebaseVisionImageMetadata.ROTATION_270)
                .build()

            var buffer = image.planes[0].buffer
            //머신러닝이 손쉽게 인식할 수 있는 사진? 모델?
            var bufferImage = FirebaseVisionImage.fromByteBuffer(buffer,metaData)

            FirebaseVision.getInstance().getVisionFaceDetector(option).detectInImage(bufferImage).addOnSuccessListener {
                faces ->
                Handler(Looper.getMainLooper()).post {
                    for (face in faces){
                        makeLens(face, bitmap)
                    }
                    binding.dectionImageview.setImageBitmap(bitmap)
                    lastBitmap = bitmap
                    image.close()
                }
            }
        }
    }
}
