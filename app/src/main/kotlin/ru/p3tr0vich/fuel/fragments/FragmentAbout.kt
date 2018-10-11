package ru.p3tr0vich.fuel.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ru.p3tr0vich.fuel.BuildConfig
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.factories.FragmentFactory
import ru.p3tr0vich.fuel.utils.UtilsFormat

class FragmentAbout : FragmentBase(FragmentFactory.Ids.ABOUT) {

    override val titleId: Int
        get() = R.string.title_about

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val versionName: String = try {
            activity?.packageManager?.getPackageInfo(activity?.packageName, 0)?.versionName ?: "0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0"
        }

        with(inflater.inflate(R.layout.fragment_about, container, false)){
            (findViewById<View>(R.id.text_app_version) as TextView).text = getString(R.string.about_version, versionName)
            (findViewById<View>(R.id.text_app_build_date) as TextView).text =
                    UtilsFormat.dateToString(BuildConfig.BUILD_DATE, true)

            return this
        }
    }
}