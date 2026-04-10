package cn.fred.tv.dialog

import android.view.View
import android.widget.AdapterView
import cn.fred.tv.R
import cn.fred.tv.adapter.ChannelAdapter
import cn.fred.tv.adapter.GroupAdapter
import cn.fred.tv.data.Channel
import cn.fred.tv.databinding.DialogChannelBinding

class ChannelListDialog(private val tvs: Set<Channel>?, private val tv: Channel?) :
    BaseDialogFragment<DialogChannelBinding>() {
    var onChoose: ((tv: Channel) -> Unit)? = null

    override fun initLayoutResource(): Int = R.layout.dialog_channel

    override fun onResume() {
        super.onResume()
        tvs?.let { tvs ->
            val map = tvs.groupBy { it.group.uppercase() }
            val groups = arrayListOf<String>()
            groups.addAll(map.keys)
            binding.lvGroup.adapter = context?.let { GroupAdapter(it, groups) }
            var index = groups.indexOf(tv?.group?.uppercase())
            if (index == -1) {
                index = 0
            }
            binding.lvGroup.setSelection(index)
            val channelAdapter = context?.let { ChannelAdapter(it) }
            binding.lvChannel.adapter = channelAdapter
            map[groups[index]]?.let {
                channelAdapter?.setChannelList(it)
                binding.lvChannel.setSelection(it.indexOf(tv))
            }
            binding.lvGroup.setOnItemClickListener { _, _, i, _ ->
                map[groups[i]]?.let {
                    channelAdapter?.setChannelList(it)
                    binding.lvChannel.setSelection(0)
                }
            }

            binding.lvGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    map[groups[position]]?.let {
                        channelAdapter?.setChannelList(it)
                        binding.lvChannel.setSelection(0)
                        // 当项被选中时执行的代码
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // 当没有项被选中时执行的代码（通常不需要实现）
                }

            }
            binding.lvChannel.setOnItemClickListener { _, _, i, _ ->
                channelAdapter?.getChannelList()?.let {
                    onChoose?.invoke(it[i])
                }
                dismiss()
            }
        }
    }
}