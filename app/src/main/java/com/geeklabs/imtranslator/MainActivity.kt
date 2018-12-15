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
import android.support.annotation.NonNull
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.AdapterView
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
    private val TAKE_PHOTO_REQUEST: Int = 5
    private var feature = Feature()
    private lateinit var prefUtil: PrefUtil
    private lateinit var mTTS: TextToSpeech
    private lateinit var toSpeak: String
    private lateinit var selectedLanguageCode: String
    private lateinit var languages: String
    private val visionAPI = arrayOf("LANDMARK_DETECTION", "LOGO_DETECTION", "SAFE_SEARCH_DETECTION", "IMAGE_PROPERTIES", "LABEL_DETECTION")
    private val api = visionAPI[4]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // to report crashes if any
            Fabric.with(this, Crashlytics())
            resultLL.visibility = View.GONE

            feature.type = api
            feature.maxResults = 6

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

            speaker.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    mTTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                else
                    mTTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

            }

            if (languages.isEmpty()) { // if not found then fetch from server

                doAsync {
                    val translate = TranslateOptions.newBuilder().setApiKey(getString(R.string.api_key)).build().getService();
                    val target = Translate.LanguageListOption.targetLanguage("en");
                    val languages = translate.listSupportedLanguages(target);

                    /* for (language in languages) {
                     out.printf("Name: %s, Code: %s\n", language.name, language.code);
                 }*/
                    val gson = Gson()
                    val fromJson = gson.toJson(languages)
                    prefUtil.lanagues = fromJson

                    uiThread {
                        this@MainActivity.languages = fromJson
                        addLanguagesToSpinner(languages)
                    }
                }
            } else {
                val gson = Gson()
                val languageList = gson.fromJson(languages, Array<com.google.cloud.translate.Language>::class.java).asList()
                addLanguagesToSpinner(languageList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showProgress() {
        if (!progress.isShown)
            progress.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        if (progress.isShown)
            progress.visibility = View.GONE
    }

    private fun addLanguagesToSpinner(languageList: List<com.google.cloud.translate.Language>) {
        try {
            val customAdapter = CustomAdapter(this, languageList)
            sp_language.adapter = customAdapter

            if (!selectedLanguageCode.isEmpty()) {
                val index = languageList.indexOfFirst { (it.code == selectedLanguageCode) }
                sp_language.setSelection(index)
            }

            hideProgress()
        } catch (ex: Exception) {
            hideProgress()
            ex.printStackTrace()
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
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.fromFile(File(photoUri.path)));
            processImage(bitmap, feature)
        }
    }


    private fun processImage(bitmap: Bitmap, feature: Feature) {

        try {
            showProgress()
            val featureList = ArrayList<Feature>();
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

                    val requestInitializer = VisionRequestInitializer(getString(R.string.api_key));

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
                    hideProgress()
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (e: IOException) {
                    hideProgress()
                    Log.d(TAG, "failed to make API request because of other IOException " + e.message);
                }
//               return "Cloud Vision API request failed. Check logs for details.";
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgress()
        }
    }

    @NonNull
    fun getImageEncodeImage(bitmap: Bitmap): Image {
        val base64EncodedImage = Image()
        // Convert the bitmap to a JPEG
        // Just in case it's a format that Android understands but Cloud Vision
        val byteArrayOutputStream = ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        val imageBytes = byteArrayOutputStream.toByteArray();

        // Base64 encode the JPEG
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }


    private fun convertResponseToString(response: BatchAnnotateImagesResponse): String {

        val imageResponses = response.getResponses().get(0);
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

    private fun formatAnnotation(entityAnnotation: List<EntityAnnotation>): String {
        try {
            var message = "";
            val resultList: MutableList<ImageResult> = mutableListOf()

            if (!entityAnnotation.isEmpty()) {

                for (entity: EntityAnnotation in entityAnnotation) {
                    message = message + "    " + entity.description + " " + entity.score;
                    message += "@";
                    val imageResult = ImageResult("" + entity.description, "" + entity.score)
                    resultList.add(imageResult)
                }

                sp_language.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        showProgress()
                        processTranslate(message);
                    }

                }

                // Translates some text into Russian
                processTranslate(message);

            } else {
                message = "Nothing Found";
                hideProgress()
            }
//        message = message.replace("@", "\n")
            runOnUiThread {
                // Creates a vertical Layout Manager
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.setHasFixedSize(true);
                recyclerView.isNestedScrollingEnabled = false;

                // You can use GridLayoutManager if you want multiple columns. Enter the number of columns as a parameter.
//        recyclerView.layoutManager = GridLayoutManager(this, 2)

                // Access the RecyclerView Adapter and load the data into it
                recyclerView.adapter = ResultAdapter(resultList, this)
                recyclerView.adapter.notifyDataSetChanged()

                hideProgress()
                resultLL.visibility = View.VISIBLE
            }
            return message
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgress()
        }
        return ""
    }

    private fun processTranslate(message: String) {
        try {
            toSpeak = message
            if (!toSpeak.isEmpty())
                speaker.visibility = View.VISIBLE

            val selectedItem = sp_language.selectedItem as Language
            val gson = Gson()
            val languageList = gson.fromJson(languages, Array<com.google.cloud.translate.Language>::class.java).asList()
            val language1 = languageList.first { it.code == selectedItem.code }
            prefUtil.selectedLanguageCode = language1.code

            doAsync {
                val language = selectedItem as Language
                val translate = TranslateOptions.newBuilder().setApiKey(getString(R.string.api_key)).build().service;
                val translation =
                        translate.translate(
                                message,
                                Translate.TranslateOption.sourceLanguage("en"),
                                Translate.TranslateOption.targetLanguage(language.code));

                var translatedText = translation.translatedText;
                translatedText = translatedText.replace("@", "\n")
                uiThread {
                    tv_translated_text.text = translatedText
                    hideProgress()
                }
            }
        } catch (e: Exception) {
            hideProgress()
            e.printStackTrace()
        }
    }

    override fun onPause() {
        if (mTTS.isSpeaking) {
            //if speaking then stop
            mTTS.stop()
        }
        super.onPause()
    }

    override fun onDestroy() {
        mTTS.stop()
        mTTS.shutdown()
        super.onDestroy()
    }
}
