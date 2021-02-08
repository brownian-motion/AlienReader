package com.brownian.alienreader.view;

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.brownian.alienreader.R
import com.brownian.alienreader.message.ReadableThing

public class SentenceAdapter(
    val layoutInflater : LayoutInflater,
    var items : List<ReadableThing> = emptyList(),
    var currentlyPlaying : Int = 0,
    var paused : Boolean = true
) : RecyclerView.Adapter<SentenceAdapter.ViewHolder>() {
    data class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val text : TextView = view.findViewById(R.id.sentence_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            layoutInflater.inflate(
                R.layout.sentence_list_item,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = (if(!paused && position == currentlyPlaying) "> " else "") + items[position].body
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            holder.text.tooltipText = items[position].body
        }
        holder.view.setBackgroundColor((if(position == currentlyPlaying) Color.LTGRAY else Color.WHITE))
    }

    override fun getItemCount(): Int = items.size
}
