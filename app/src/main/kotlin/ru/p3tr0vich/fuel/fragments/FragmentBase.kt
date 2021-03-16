package ru.p3tr0vich.fuel.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import ru.p3tr0vich.fuel.ImplementException
import ru.p3tr0vich.fuel.helpers.PreferencesHelper

abstract class FragmentBase(override val fragmentId: Int) : Fragment(), FragmentInterface {

    private var onFragmentChangeListener: FragmentInterface.OnFragmentChangeListener? = null

    protected lateinit var preferencesHelper: PreferencesHelper

    override val titleId: Int
        get() = -1

    override val title: String?
        get() {
            val id = titleId
            return if (id != -1) getString(id) else null
        }

    override val subtitleId: Int
        get() = -1

    override val subtitle: String?
        get() {
            val id = subtitleId
            return if (id != -1) getString(id) else null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferencesHelper = PreferencesHelper.getInstance(requireContext())

        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onFragmentChangeListener = context as FragmentInterface.OnFragmentChangeListener?
        } catch (e: ClassCastException) {
            throw ImplementException(context, FragmentInterface.OnFragmentChangeListener::class.java)
        }

    }

    override fun onStart() {
        super.onStart()
        onFragmentChangeListener?.onFragmentChange(this)
    }

    override fun onBackPressed(): Boolean {
        return false
    }
}