package zed.rainxch.githubstore.feature.search.domain.model;

enum class RootFilterType {
    All,
    RequiresRoot,
    MagiskModule,
    LsposedModule;

    fun displayText(): String = when (this) {
        All -> "All Apps"
        RequiresRoot -> "Root Required"
        MagiskModule -> "Magisk Module"
        LsposedModule -> "LSPosed Module"
    }

    fun searchKeywords(): List<String> = when (this) {
        All -> emptyList()
        RequiresRoot -> listOf("root", "superuser", "su")
        MagiskModule -> listOf("magisk", "magisk module")
        LsposedModule -> listOf("lsposed", "xposed", "lsposed module", "xposed module")
    }
}