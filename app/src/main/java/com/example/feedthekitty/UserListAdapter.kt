package com.example.feedthekitty

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class UserListAdapter(private val mContext: Context) : BaseAdapter() {
    internal class Item(
        var email: String = "",
        var amount: String = ""
    )
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(mContext)
    private val mItems =ArrayList<Item>()

    fun add(email: String, amount: String){
        Log.i("ftk", email)
        mItems.add(Item(email.replace('^','.'), amount))
    }
    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val cur = getItem(pos) as Item
        val newView: View

        val viewHolder: ViewHolder
        if(null == convertView){
            newView = mLayoutInflater.inflate(R.layout.user_list_view_item, parent, false)

            viewHolder = ViewHolder()
            newView.tag = viewHolder
            viewHolder.amountView = newView.findViewById(R.id.amountView)
            viewHolder.checkView = newView.findViewById(R.id.checkmarkImage)
            viewHolder.emailView = newView.findViewById(R.id.userName)
        }
        else {

            newView = convertView
            viewHolder = newView.tag as ViewHolder
        }

        viewHolder.emailView.text = cur.email
        viewHolder.amountView.text = cur.amount
        viewHolder.checkView.setImageResource(R.drawable.green_checkmark)

        if(cur.amount == "$0"){
            viewHolder.checkView.visibility = View.INVISIBLE
        } else{
            viewHolder.checkView.visibility = View.VISIBLE
        }

        return newView
    }

    override fun getItem(p0: Int): Any {
       return mItems[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getCount(): Int {
        return mItems.size
    }

    internal class ViewHolder{
        lateinit var emailView: TextView
        lateinit var amountView: TextView
        lateinit var checkView: ImageView
    }
}