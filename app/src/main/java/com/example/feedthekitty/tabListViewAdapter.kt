package com.example.feedthekitty

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


// Layout taken from class examples
class tabListViewAdapter (context: Context, resource: Int, objects: ArrayList<PaymentTabs.uidAndTab>): ArrayAdapter<PaymentTabs.uidAndTab>(context, resource, objects){

    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(context)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var newView: View

        if (null == convertView) {

            // Not recycled. Create the View
            newView = mLayoutInflater.inflate(R.layout.tab_badge_view, parent, false)

            // Cache View information in ViewHolder Object
            val viewHolder = ViewHolder()
            newView.tag = viewHolder
            viewHolder.nameView = newView.findViewById(R.id.owe_name)
            viewHolder.valueView = newView.findViewById(R.id.owe_value)
            viewHolder.closedView = newView.findViewById(R.id.closedText)

        } else {
            newView = convertView
        }
        val storedViewHolder = newView.tag as ViewHolder

        //Set the data in the data View
        storedViewHolder.nameView.text = getItem(position).tab.eventName
        storedViewHolder.valueView.text = "$" + getItem(position).tab.balance + "/" + "$" + getItem(position).tab.totalRequested
        storedViewHolder.tab = getItem(position).tab
        storedViewHolder.uid = getItem(position).uid
        if(getItem(position).tab.balance == getItem(position).tab.totalRequested){
            storedViewHolder.closedView.text = "CLOSED"
        }
        return newView

    }

    internal class ViewHolder {
        lateinit var nameView: TextView
        lateinit var closedView: TextView
        lateinit var valueView: TextView
        lateinit var  tab: Tab
        lateinit var uid:String
    }


}
