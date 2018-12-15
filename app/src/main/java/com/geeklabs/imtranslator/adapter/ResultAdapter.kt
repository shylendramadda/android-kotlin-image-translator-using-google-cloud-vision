package com.geeklabs.imtranslator.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import antonkozyriatskyi.circularprogressindicator.PatternProgressTextAdapter
import com.geeklabs.imtranslator.model.ImageResult
import com.geeklabs.imtranslator.R
import kotlinx.android.synthetic.main.result_custom_row.view.*

class ResultAdapter(val items : List<ImageResult>, val context: Context) : RecyclerView.Adapter<ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.result_custom_row, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tv_desc.text = items[position].resultText
        holder.circleView?.setProgress(items[position].resultValue.toDouble(), 1.0)
        holder.circleView.setProgressTextAdapter(PatternProgressTextAdapter("%.2f"));
    }

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return items.size
    }

}

class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each animal to
        val tv_desc = view.tv_desc!!
        val circleView = view.circleView
}