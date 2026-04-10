package cn.fred.tv.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.fred.tv.R

class GroupAdapter(private val context: Context, private val groups: List<String>) : BaseAdapter() {
    override fun getCount(): Int = groups.size

    override fun getItem(p0: Int): String = groups[p0]

    override fun getItemId(p0: Int): Long = p0.toLong()

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val holder: GroupHolder
        var view: View? = p1
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_group, p2, false)
            holder = GroupHolder(view)
        } else {
            holder = view.tag as GroupHolder
        }
        holder.textView.text = groups[p0]
        return view!!
    }

    inner class GroupHolder(view: View) {
        val textView: TextView = view.findViewById(R.id.tv_group)

        init {
            view.tag = this
        }
    }
}