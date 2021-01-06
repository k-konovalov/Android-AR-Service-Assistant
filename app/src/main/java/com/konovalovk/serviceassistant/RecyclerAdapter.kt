package com.konovalovk.serviceassistant

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter : RecyclerView.Adapter<ActorViewHolder>() {
    var ports: List<Port> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = ports.size
    fun getItem(position: Int) : Port = ports[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActorViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ActorViewHolder(inflater.inflate(R.layout.port_item, parent, false))
    }

    override fun onBindViewHolder(holder: ActorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ActorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val id = itemView.findViewById<TextView>(R.id.tvItemId)
    private val body = itemView.findViewById<ImageView>(R.id.ivBackground)

    @SuppressLint("UseCompatLoadingForDrawables")
    fun bind(port: Port) {
        //avatar.setImageResource(actor.avatar)
        id.text = port.number
        body.setImageResource(if (port.isEnabled) R.drawable.ic_info else R.drawable.ic_close)
        body.setColorFilter(if (port.isEnabled) Color.parseColor("#26ED70") else Color.parseColor("#AC4848"))

    }
}