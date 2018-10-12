package ru.p3tr0vich.fuel.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.View
import ru.p3tr0vich.fuel.DividerItemDecorationPreferences
import ru.p3tr0vich.fuel.ImplementException
import ru.p3tr0vich.fuel.helpers.PreferencesHelper

abstract class FragmentPreferencesBase(override val fragmentId: Int) : PreferenceFragmentCompat(), FragmentInterface {

    private var onFragmentChangeListener: FragmentInterface.OnFragmentChangeListener? = null

    protected lateinit var preferencesHelper: PreferencesHelper

    override val titleId: Int
        get() = -1

    override val subtitleId: Int
        get() = -1

    override val subtitle: String?
        get() {
            val id = subtitleId
            return if (id != -1) getString(id) else null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesHelper = PreferencesHelper.getInstance(context!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: check
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setDivider(null)
            listView.addItemDecoration(DividerItemDecorationPreferences(context))
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            onFragmentChangeListener = context as FragmentInterface.OnFragmentChangeListener?
        } catch (e: ClassCastException) {
            throw ImplementException(context!!, FragmentInterface.OnFragmentChangeListener::class.java)
        }

    }

    override fun onStart() {
        super.onStart()
        onFragmentChangeListener?.onFragmentChange(this)
    }
}