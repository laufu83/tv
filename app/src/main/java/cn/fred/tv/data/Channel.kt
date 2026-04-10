package cn.fred.tv.data

import java.io.Serializable


data class Channel(
    val id: String = "",
    val name: String = "",
    val group: String = "",
    val logo: String = "",
    var url: String = ""
) : Comparable<Channel>,
    Serializable {
    override fun compareTo(other: Channel): Int {
        return other.group.last() - this.group.last()
    }

}
