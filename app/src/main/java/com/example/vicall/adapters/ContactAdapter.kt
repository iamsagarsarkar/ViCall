package com.example.vicall.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.vicall.R
import com.example.vicall.activities.CallActivity
import com.example.vicall.models.ContactModel
import com.example.vicall.onClick.CallOnClick
import kotlinx.android.synthetic.main.item_contact.view.*
import java.net.URL
import java.util.concurrent.Executors

open class ContactAdapter(private val context: Context,private val onClick: CallOnClick,private val list: ArrayList<ContactModel>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_contact,parent,false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if (holder is MyViewHolder){
            holder.itemView.tv_contact_name.text = model.userName
            holder.itemView.tv_contact_number.text = model.userPhone
            getBitmapFromURL(holder,model.userImage.toString())
            holder.itemView.iv_contact_call.setOnClickListener {
                onClick.onSelectedLCallClick(model)
            }
        }
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
                    holder.itemView.iv_contact_image.setImageBitmap(image)
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}