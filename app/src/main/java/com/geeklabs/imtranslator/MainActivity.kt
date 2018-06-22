package com.geeklabs.imtranslator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.NonNull
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.desmond.squarecamera.CameraActivity
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAKE_PHOTO_REQUEST: Int = 560
    private lateinit var feature: Feature
    private lateinit var bitmap: Bitmap
    private val visionAPI = arrayOf("LANDMARK_DETECTION", "LOGO_DETECTION", "SAFE_SEARCH_DETECTION", "IMAGE_PROPERTIES", "LABEL_DETECTION")

    val apiKey :String  ="MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCyhZkZA2Oyf2sk\\nOAISv9o/J9Gwu59UHsd4tXbQjEpzlLvlMz4C4XWV2CR6bTlaLpKUfhTFZNXzs1Zg\\n3mKgOb3sn9Cgerpn7ILqSzqf/CSUS1WVoEtDRMbcsWiJDODDpK21VFLHv0cu6gjU\\nY8lWex//kBRhEV+/nb7u/QbUpxZYPr0+kZClCEEzHy6C/WWG623qu5efSmPnPHRD\\n+HlsIOexdeFi/nPWGyby8wH6gPOekuimRUGHA5prApNth33NzXoEQYW98OofJpm4\\nRjPuOE0hFgAw2c4P30yjCGMhr41j0MiUkkxQosCpKZFlzfHmSnJlTNbmU4haSSvx\\n+DZHkm2LAgMBAAECggEAJf7aRWEbzIdjCdvj0RLFRDDY5+ke8Zv1b4MLzTo2tF/h\\nF9iup5VN3f0ZUndBwChuaS1mhVa/VLWEOmzKh/iSLDUdhbJpTyoe+PfW++sB7BAW\\noJhzvFb8jkcyDQ1RH0LC9/eBAON8pocIJxAv73iYKGAFfl1gyBsuYpY26HbBgjlI\\nc6E81hMVFwVk/lE0U7AbKLbOlxCqM3gSkOuZF9av8g+VbgQKyKptWPFoBRBVLnT2\\nRruXZhTRavIsEQNtwAQ0wLVXB+u2lQN7uGvkFi4craLAAzmTcbWEudUVlHBryO/X\\n6SXjDBe67GHD1kmgWakurfl3XjltHeTq5MC71TCpbQKBgQDzJKZxFtE1K/0TQUGd\\nPzpq/Ue15KbxyFUvv4b/wbOVXZIUSl+TyR0iaoJDKAt+ZAWt3xtPQP8/6/8wTj3O\\nfcG5B74NWJ6MyoqOaxudJLgLzZkoqp2qDdkI94iAQCxGMhMW4B/kjOYJXEaSSgu0\\np2k03Lcy3IH0Vw+ia321fN/xPwKBgQC79jC/MgKJDq07nBCB42ITtwIo2aQ9QHUO\\nC4wrdrri5DpwD9R2DEEW3sdMxpPrds19OOa3UdwrS8T1XdOYZlGAF0ewfy+mPmRK\\nu2AdtaX5QfPVExzYZYSb8Ugx6yz+1tzVTRXRorSYK9Q+5OR3KcOMOrMu5TeIhIEX\\nVmsTXLAktQKBgBkv8cIDUBbHAMdu2iI0+5M7u6L/FcA0NYblu1FhOn49nDVX4wDH\\nM6puCCJ20oH8UI5Lb2PNYuO3Sc8yO7rZUikdwTVWuc3x6VqJg+nKdPpcCQKqcfy8\\nxH/mTJCklTGMXGfhPcyKQAY2NeVPoFjNgtuEBcJSD3BFWIxFwFb9oaE3AoGAE7h0\\nzNqWYYLksghhwv70X1UoKNkM3lBQ97RGdJj0arG/X9qJVAldGuUsy+VZx66jSKwb\\nqMgx7Wj5tTSu6qJxkprerqnpeeu54g1evD8+trQwvP5QXHPqQeJCzNn70pEAgnCg\\nBWqov/55OlARmF8NYT0XZ6gs92nPkX9DpLho0rECgYBjHOnSWk54MYmMDzljhbWg\\n0CsZBGlbVb1Bgo5i9f3OB48gEj5A+wwUAEVEYa1BJmPJfNfFUMjQGIPq8ibJntWP\\nuLhrbq4GaqanpZeQPWkmXG7gwD4dqQAiypJK3Vnug6GUEiklOPLdytY/Vq4eauXc\\nfa3bIdxWsxUH3h0s8KjnDA=="

    private val api = visionAPI[4]

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

            //getDataFromImage(photoUri.path)
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            processImage(bitmap, feature)
        }
    }

    private fun processImage(bitmap: Bitmap, feature: Feature) {

        var featureList = ArrayList<Feature>();
        featureList.add(feature);

        val annotateImageRequests = ArrayList<AnnotateImageRequest>();
        val annotateImageReq = AnnotateImageRequest();
        annotateImageReq.setFeatures(featureList);
        annotateImageReq.setImage(getImageEncodeImage(bitmap));
        annotateImageRequests.add(annotateImageReq);
        doAsync {
            try {

                val httpTransport = AndroidHttp.newCompatibleTransport();
                val jsonFactory = GsonFactory.getDefaultInstance();

                val requestInitializer = VisionRequestInitializer(apiKey);

                val builder = Vision.Builder(httpTransport, jsonFactory, null);
                builder.setVisionRequestInitializer(requestInitializer);

                val vision = builder.build();

                val batchAnnotateImagesRequest = BatchAnnotateImagesRequest();
                batchAnnotateImagesRequest.setRequests(annotateImageRequests);

                val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                annotateRequest.setDisableGZipContent(true);
                val response = annotateRequest.execute();

                convertResponseToString(response)
            } catch (e: GoogleJsonResponseException) {
                // Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (e: IOException) {
                // Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
            }
            //   return "Cloud Vision API request failed. Check logs for details.";
        }
    }

    @NonNull
    fun getImageEncodeImage(bitmap: Bitmap): Image {
        val base64EncodedImage = Image();
        // Convert the bitmap to a JPEG
        // Just in case it's a format that Android understands but Cloud Vision
        val byteArrayOutputStream = ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        val imageBytes = byteArrayOutputStream.toByteArray();

        // Base64 encode the JPEG
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }


    fun convertResponseToString(response: BatchAnnotateImagesResponse): String {

        val imageResponses = response.getResponses().get(0);

        var entityAnnotations = null;

        var message = "";

        if (api.equals("LABEL_DETECTION")) {
            message = formatAnnotation(imageResponses.getLabelAnnotations());
            return message;
        }
        /*switch (api) {
            case "LANDMARK_DETECTION":
                entityAnnotations = imageResponses.getLandmarkAnnotations();
                message = formatAnnotation(entityAnnotations);
                break;
            case "LOGO_DETECTION":
                entityAnnotations = imageResponses.getLogoAnnotations();
                message = formatAnnotation(entityAnnotations);
                break;
            case "SAFE_SEARCH_DETECTION":
                SafeSearchAnnotation annotation = imageResponses.getSafeSearchAnnotation();
                message = getImageAnnotation(annotation);
                break;
            case "IMAGE_PROPERTIES":
                ImageProperties imageProperties = imageResponses.getImagePropertiesAnnotation();
                message = getImageProperty(imageProperties);
                break;
            case "LABEL_DETECTION":
                entityAnnotations = imageResponses.getLabelAnnotations();
                message = formatAnnotation(entityAnnotations);
                break;
        }*/
        return message;
    }

    fun formatAnnotation(entityAnnotation: List<EntityAnnotation>): String {
        var message = "";

        if (entityAnnotation != null) {
            for (entity: EntityAnnotation in entityAnnotation) {
                message = message + "    " + entity.getDescription() + " " + entity.getScore();
                message += "\n";
            }
        } else {
            message = "Nothing Found";
        }
        tv_desc.text = message
        return message;
    }
}
