package desu.inugram.helpers.plugins

object PythonPluginTitles {

    @JvmStatic
    fun titleOf(perm: String?): CharSequence {
        return perm ?: ""
    }

    @JvmStatic
    fun shortTitleOf(perm: String?): CharSequence {
        return perm ?: ""
    }
}
