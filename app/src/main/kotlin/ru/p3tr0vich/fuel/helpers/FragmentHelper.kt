package ru.p3tr0vich.fuel.helpers

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.factories.FragmentFactory
import ru.p3tr0vich.fuel.fragments.FragmentCalc
import ru.p3tr0vich.fuel.fragments.FragmentFueling
import ru.p3tr0vich.fuel.fragments.FragmentInterface
import ru.p3tr0vich.fuel.fragments.FragmentPreferences

class FragmentHelper(private val fragmentActivity: FragmentActivity) {

    val currentFragment: FragmentInterface
        get() {
            return fragmentActivity.supportFragmentManager?.findFragmentById(R.id.content_frame) as FragmentInterface
        }

    val fragmentFueling: FragmentFueling?
        get() = getFragment(FragmentFactory.Ids.FUELING) as FragmentFueling?

    val fragmentCalc: FragmentCalc?
        get() = getFragment(FragmentFactory.Ids.CALC) as FragmentCalc?

    val fragmentPreferences: FragmentPreferences?
        get() = getFragment(FragmentFactory.Ids.PREFERENCES) as FragmentPreferences?

    fun getFragment(@FragmentFactory.Ids.Id fragmentId: Int): Fragment? {
        return fragmentActivity.supportFragmentManager?.findFragmentByTag(FragmentFactory.fragmentIdToTag(fragmentId))
    }

    fun addMainFragment() {
        fragmentActivity.supportFragmentManager?.beginTransaction()
                ?.add(R.id.content_frame,
                        FragmentFactory.getFragmentNewInstance(FragmentFactory.Ids.MAIN),
                        FragmentFactory.fragmentIdToTag(FragmentFactory.Ids.MAIN))
                ?.setTransition(FragmentTransaction.TRANSIT_NONE)
                ?.commit()
    }

    fun replaceFragment(@FragmentFactory.Ids.Id fragmentId: Int, args: Bundle?) {
        fragmentActivity.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.content_frame,
                        FragmentFactory.getFragmentNewInstance(fragmentId, args),
                        FragmentFactory.fragmentIdToTag(fragmentId))
                ?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                ?.addToBackStack(null)
                ?.commit()
    }
}