package zed.rainxch.githubstore.core.data.model

sealed class InstallationProgress {
    object Preparing : InstallationProgress()
    object CreatingSession : InstallationProgress()
    data class Installing(val progress: Int) : InstallationProgress()
    object Finalizing : InstallationProgress()
    data class Success(val packageName: String) : InstallationProgress()
    data class Error(val message: String) : InstallationProgress()
}