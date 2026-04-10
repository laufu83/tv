package cn.fred.tv.data

sealed class DownloadStatus {
    data class DownLoading(val progress: Int) : DownloadStatus()
    data class Fail(val code: Int) : DownloadStatus()
    data class Error(val error: Throwable) : DownloadStatus()
    data object Success : DownloadStatus()
}