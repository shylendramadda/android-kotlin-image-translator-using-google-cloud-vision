package com.geeklabs.imtranslator

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.desmond.squarecamera.CameraActivity
import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.Feature.Type
import com.google.cloud.vision.v1.Image
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.protobuf.ByteString
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAKE_PHOTO_REQUEST: Int = 560
//    val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Fabric.with(this, Crashlytics())
        iv_photo.setOnClickListener {
            openCameraIntent()
        }

    }

    private fun openCameraIntent() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val checkSelfPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
                val PERMISSION_CAMERA = 0
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_CAMERA)
            } else {
                // Start CameraActivity
                val startCustomCameraIntent = Intent(this, CameraActivity::class.java)
                startActivityForResult(startCustomCameraIntent, TAKE_PHOTO_REQUEST)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && TAKE_PHOTO_REQUEST == requestCode) {
            val photoUri = data!!.getData()
            Log.i("URL", photoUri.path)
            Glide.with(this).load(photoUri).into(iv_photo)

            getDataFromImage(photoUri.path)
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun getDataFromImage(fileName: String) {
        doAsync {
            // Instantiates a client
            ImageAnnotatorClient.create().use { vision ->

                // The path to the image file to annotate
//            val fileName = "./resources/wakeupcat.jpg"

                // Reads the image file into memory
                val path = Paths.get(fileName)
                val data = Files.readAllBytes(path)
                val imgBytes = ByteString.copyFrom(data)

                // Builds the image annotation request
                val requests = ArrayList<AnnotateImageRequest>()
                val img = Image.newBuilder().setContent(imgBytes).build()
                val feat = Feature.newBuilder().setType(Type.LABEL_DETECTION).build()
                val request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feat)
                        .setImage(img)
                        .build()
                requests.add(request)

                // Performs label detection on the image file
                val response = vision.batchAnnotateImages(requests)
                val responses = response.responsesList

                for (res in responses) {
                    if (res.hasError()) {
                        uiThread {
                            System.out.printf("Error: %s\n", res.error.message)
                            tv_desc.text = "" + res.error.message
                        }
//                        return
                    }

                    for (annotation in res.labelAnnotationsList) {
                        annotation.allFields.forEach { k, v ->
                            uiThread {
                                System.out.printf("%s : %s\n", k, v.toString())
                                tv_desc.text = v.toString()
                            }
                        }
                    }
                }
            }
        }
    }


    /*fun run(url: String) {
        val request = Request.Builder()
                .url(url)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) = println(response.body()?.string())
        })
    }*/


}
