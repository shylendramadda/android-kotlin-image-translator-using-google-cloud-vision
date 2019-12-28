package com.geeklabs.imtranslator.adapter

import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import antonkozyriatskyi.circularprogressindicator.PatternProgressTextAdapter
import com.geeklabs.imtranslator.R
import com.geeklabs.imtranslator.model.ImageResult
import kotlinx.android.synthetic.main.result_custom_row.view.*

class ResultAdapter(private val items: List<ImageResult>,
                    private val context: Context, private val mTTS: TextToSpeech)
    : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.result_custom_row, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val resultText = items[position].resultText
            val translatedText = items[position].translatedText
            val languageCode = items[position].resultValue

            holder.tv_desc.text = resultText
            holder.tv_translated_text.text = translatedText
            holder.circleView?.setProgress(languageCode.toDouble() * 100, 100.0)
            val progressTextAdapter = PatternProgressTextAdapter("%.1f")
            holder.circleView.setProgressTextAdapter(progressTextAdapter)

            holder.speaker.setOnClickListener {
                mTTS.speak(resultText, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return items.size
    }

}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    // Holds the TextView that will add each animal to
    val tv_desc = view.tv_result_text!!
    val tv_translated_text = view.tv_translated_text!!
    val circleView = view.circleView
    val speaker = view.speaker
}