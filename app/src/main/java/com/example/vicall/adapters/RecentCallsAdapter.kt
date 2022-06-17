package com.example.vicall.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.vicall.R
import com.example.vicall.models.RecentCallModel
import com.example.vicall.onClick.OnRecentCallClick
import kotlinx.android.synthetic.main.item_recent_call.view.*
import java.net.URL
import java.util.concurrent.Executors

open class RecentCallsAdapter(private val context: Context,private val onRecentCallClick: OnRecentCallClick,private val list: ArrayList<RecentCallModel>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
       return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_recent_call,parent,false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if (holder is MyViewHolder){
            holder.itemView.tv_recent_name.text = model.userName
            holder.itemView.tv_recent_time.text = model.date
            getBitmapFromURL(holder,model.userImage.toString())
            setCallState(holder,model.callState)
            holder.itemView.iv_user_call.setOnClickListener {
                onRecentCallClick.onSelectedLRecentCallClick(model)
            }
        }
    }

    private fun setCallState(holder: MyViewHolder, callState: String?) {
        holder.itemView.iv_recent_call.setImageResource(
            when (callState) {
                "callMade" -> R.drawable.ic_baseline_call_made_24
                "callReceived" -> R.drawable.ic_baseline_call_received_24
                "callMissed" -> R.drawable.ic_baseline_call_missed_24
                else -> R.drawable.ic_baseline_call_missed_outgoing_24
            }
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }
    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)


    private fun getBitmapFromURL(holder: RecyclerView.ViewHolder,imageURL: String){
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var image: Bitmap? = null
        executor.execute {
            try {
                val `in` = URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
                handler.post {
                    holder.itemView.iv_recent_image.setImageBitmap(image)
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}