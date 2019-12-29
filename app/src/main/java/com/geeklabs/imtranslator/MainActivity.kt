package com.geeklabs.imtranslator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.desmond.squarecamera.CameraActivity
import com.geeklabs.imtranslator.adapter.CustomAdapter
import com.geeklabs.imtranslator.adapter.ResultAdapter
import com.geeklabs.imtranslator.model.ImageResult
import com.geeklabs.imtranslator.util.PrefUtil
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import com.google.cloud.translate.Language
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.gson.Gson
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG: String = "MainActivity"
    private val TAKE_PHOTO_REQUEST = 5
    private val PERMISSION_CAMERA = 1
    private var feature = Feature()
    private lateinit var prefUtil: PrefUtil
    private lateinit var mTTS: TextToSpeech
    private lateinit var selectedLanguageCode: String
    private lateinit var languages: String
    private lateinit var languageList: List<Language>
    private var resultList: MutableList<ImageResult> = mutableListOf()
    private val visionAPI = arrayOf(
        "LANDMARK_DETECTION",
        "LOGO_DETECTION",
        "SAFE_SEARCH_DETECTION",
        "IMAGE_PROPERTIES",
        "LABEL_DETECTION"
    )
    private val api = visionAPI[4]
    private lateinit var selectedItem: Language
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // to report crashes if any
            Fabric.with(this, Crashlytics())
            resultLL.visibility = View.GONE

            feature.type = api
            feature.maxResults = 10

            prefUtil = PrefUtil(this)
            languages = prefUtil.lanagues
            selectedLanguageCode = prefUtil.selectedLanguageCode

            mTTS = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
                if (status != TextToSpeech.ERROR) {
                    //if there is no error then set language
                    mTTS.language = Locale.UK
                }
            })

            iv_photo.setOnClickListener {
                openCameraIntent()
            }

            if (languages.isEmpty()) { // if not found then fetch from server

                doAsync {
                    val translate =
                        TranslateOptions.newBuilder().setApiKey(getString(R.string.api_key)).build()
                            .service
                    val target = Translate.LanguageListOption.targetLanguage("en")
                    val languages = translate.listSupportedLanguages(target)

                    /* for (language in languages) {
                     out.printf("Name: %s, Code: %s\n", language.name, language.code)
                 }*/
                    val fromJson = Gson().toJson(languages)
                    prefUtil.lanagues = fromJson

                    uiThread {
                        addLanguagesToSpinner(languages)
                    }
                }
            } else {
                val gson = Gson()
                val languageList = gson.fromJson(languages, Array<Language>::class.java).asList()
                addLanguagesToSpinner(languageList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgress()
            showErrorMessage()
        }
    }

    private fun showErrorMessage() {
        Toast.makeText(this, "Something went wrong please try again.", Toast.LENGTH_SHORT).show()
    }

    private fun showProgress() {
        runOnUiThread {
            progress.visibility = View.VISIBLE
        }
    }

    private fun hideProgress() {
        runOnUiThread {
            progress.visibility = View.GONE
        }
    }

    private fun addLanguagesToSpinner(languageList: List<Language>) {
        try {
            val customAdapter = CustomAdapter(this, languageList)
            spinner_language.adapter = customAdapter
            this.languageList = languageList

            if (!selectedLanguageCode.isEmpty()) {
                val index = languageList.indexOfFirst { (it.code == selectedLanguageCode) }
                spinner_language.setSelection(index)
            } else {
                val index = languageList.indexOfFirst { (it.code == "en") }
                spinner_language.setSelection(index)
            }

            hideProgress()
        } catch (ex: Exception) {
            hideProgress()
            showErrorMessage()
            ex.printStackTrace()
        }
    }

    private fun openCameraIntent() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val checkSelfPermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_CAMERA
                )
            } else {
                // Start CameraActivity
                val startCustomCameraIntent = Intent(this, CameraActivity::class.java)
                startActivityForResult(startCustomCameraIntent, TAKE_PHOTO_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_CAMERA -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user")
                    finish()
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                    openCameraIntent()
                }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && TAKE_PHOTO_REQUEST == requestCode) {
            photoUri = data!!.data
            if (photoUri != null) {
                resultList.clear() // remove previous results from list
                Log.i("URL", photoUri?.path!!)
                Glide.with(this).load(photoUri).into(iv_photo)

                //getDataFromImage(photoUri.path)
                val bitmap = MediaStore.Images.Media.getBitmap(
                    this.contentResolver,
                    Uri.fromFile(File(photoUri?.path!!))
                )
                processImage(bitmap, feature)
            } else {
                Toast.makeText(this, "Unable to get file path", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun processImage(bitmap: Bitmap, feature: Feature) {

        try {
            showProgress()
            val featureList = ArrayList<Feature>()
            featureList.add(feature)

            val annotateImageRequests = ArrayList<AnnotateImageRequest>()
            val annotateImageReq = AnnotateImageRequest()
            annotateImageReq.features = featureList
            annotateImageReq.image = this.getImageEncodeImage(bitmap)
            annotateImageRequests.add(annotateImageReq)

            doAsync {
                try {

                    val httpTransport = AndroidHttp.newCompatibleTransport()
                    val jsonFactory = GsonFactory.getDefaultInstance()

                    val requestInitializer = VisionRequestInitializer(getString(R.string.api_key))

                    val builder = Vision.Builder(httpTransport, jsonFactory, null)
                    builder.setVisionRequestInitializer(requestInitializer)

                    val vision = builder.build()

                    val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
                    batchAnnotateImagesRequest.requests = annotateImageRequests

                    val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest)
                    annotateRequest.disableGZipContent = true
                    val response = annotateRequest.execute()

                    convertResponseToString(response)

                    // Delete image from file storage
                    if (photoUri != null) {
                        val file = File(photoUri?.path!!)
                        if (file.exists()) {
                            file.delete()
                        }
                    }

                } catch (e: GoogleJsonResponseException) {
                    hideProgress()
                    showErrorMessage()
                    Log.d(TAG, "failed to make API request because " + e.content)
                } catch (e: IOException) {
                    hideProgress()
                    showErrorMessage()
                    Log.d(
                        TAG,
                        "failed to make API request because of other IOException " + e.message
                    )
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorMessage()
            hideProgress()
        }
    }

    @NonNull
    fun getImageEncodeImage(bitmap: Bitmap): Image {
        val base64EncodedImage = Image()
        try {
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            showErrorMessage()
            hideProgress()
        }
        return base64EncodedImage
    }


    private fun convertResponseToString(response: BatchAnnotateImagesResponse) {

        val imageResponses = response.responses[0]

        if (api == "LABEL_DETECTION") {
            formatAnnotation(imageResponses.labelAnnotations)
        }
        /*switch (api) {
            case "LANDMARK_DETECTION":
                entityAnnotations = imageResponses.getLandmarkAnnotations()
                message = formatAnnotation(entityAnnotations)
                break
            case "LOGO_DETECTION":
                entityAnnotations = imageResponses.getLogoAnnotations()
                message = formatAnnotation(entityAnnotations)
                break
            case "SAFE_SEARCH_DETECTION":
                SafeSearchAnnotation annotation = imageResponses.getSafeSearchAnnotation()
                message = getImageAnnotation(annotation)
                break
            case "IMAGE_PROPERTIES":
                ImageProperties imageProperties = imageResponses.getImagePropertiesAnnotation()
                message = getImageProperty(imageProperties)
                break
            case "LABEL_DETECTION":
                entityAnnotations = imageResponses.getLabelAnnotations()
                message = formatAnnotation(entityAnnotations)
                break
        }*/
    }

    private fun formatAnnotation(entityAnnotation: List<EntityAnnotation>) {
        try {
            var imageTextResults = ""

            if (!entityAnnotation.isEmpty()) {

                for (entity: EntityAnnotation in entityAnnotation) {
                    imageTextResults += entity.description.toString() + "@"
                    val imageResult =
                        ImageResult(entity.description.toString(), entity.score.toString(), "")
                    resultList.add(imageResult)
                }

                spinner_language.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {

                        }

                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            processTranslate(imageTextResults)
                        }

                    }

                selectedItem = spinner_language.selectedItem as Language

                if (selectedItem.code != "en") {
                    // Translates text into selected language
                    processTranslate(imageTextResults)
                }
            } else {
                imageTextResults = "Nothing Found"
                hideProgress()
            }

            runOnUiThread {
                // Creates a vertical Layout Manager
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.setHasFixedSize(true)
                recyclerView.isNestedScrollingEnabled = false

                // You can use GridLayoutManager if you want multiple columns. Enter the number of columns as a parameter.
//        recyclerView.layoutManager = GridLayoutManager(this, 2)

                // Access the RecyclerView Adapter and load the data into it
                val resultAdapter = ResultAdapter(resultList, this, mTTS)
                recyclerView.adapter = resultAdapter
                resultAdapter.notifyDataSetChanged()

                resultLL.visibility = View.VISIBLE
                hideProgress()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorMessage()
            hideProgress()
        }
    }

    private lateinit var splits: List<String>

    private fun processTranslate(imageTextResult: String) {
        try {
            showProgress()
            selectedItem = spinner_language.selectedItem as Language
            if (selectedItem.code == "en") {
                hideProgress()
//                Toast.makeText(this, "Please choose other language", Toast.LENGTH_LONG).show()
                return
            }

            // save selected language code in local
            val language = this.languageList.first { it.code == selectedItem.code }
            prefUtil.selectedLanguageCode = language.code

            doAsync {
                val translate = TranslateOptions.newBuilder().setApiKey(getString(R.string.api_key))
                    .build().service
                val translation =
                    translate.translate(
                        imageTextResult,
                        Translate.TranslateOption.sourceLanguage("en"),
                        Translate.TranslateOption.targetLanguage(selectedItem.code)
                    )

                val translatedText = translation.translatedText

                uiThread {
                    hideProgress()

                    if (translatedText.contains("@"))
                        splits = translatedText.split("@")

                    if (!splits.isEmpty()) {

                        for (i in 0 until splits.size - 1) {
                            resultList[i].translatedText = splits[i]
                        }

                        val resultAdapter = ResultAdapter(resultList, this@MainActivity, mTTS)
                        recyclerView.adapter = resultAdapter
                        resultAdapter.notifyDataSetChanged()

                    }
                }
            }
        } catch (e: Exception) {
            hideProgress()
            showErrorMessage()
            e.printStackTrace()
        }
    }

    override fun onPause() {
        if (mTTS.isSpeaking)
            mTTS.stop()
        super.onPause()
    }

    override fun onDestroy() {
        mTTS.stop()
        mTTS.shutdown()
        super.onDestroy()
    }
}
