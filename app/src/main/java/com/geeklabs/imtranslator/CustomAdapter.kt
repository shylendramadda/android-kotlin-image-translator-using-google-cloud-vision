package com.geeklabs.imtranslator

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.google.cloud.translate.Language

public class CustomAdapter : BaseAdapter {


    private var activity: Activity? = null
    var languages: List<Language>? = null;

    constructor(activity: Activity, languages: List<Language>?) {
        this.activity = activity
        this.languages = languages

        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view: View?
        val viewHolder: ViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(activity).inflate(R.layout.item_text, parent, false)
            ViewHolder(view)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        var language = languages!![position]
        viewHolder.tvLanguageName?.text = language.name
        return view as View
    }

    override fun getItem(position: Int): Any {
        return languages!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        if (languages == null) {
            return 0
        }
        return languages!!.size
    }

    class  ViewHolder(view: View) {
        var tvLanguageName: TextView? = null

        init {
            this.tvLanguageName = view?.findViewById<TextView>(R.id.tv_language_name)
        }
    }
}

