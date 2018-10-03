package ru.p3tr0vich.fuel.helpers

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentTransaction
import ru.p3tr0vich.fuel.*
import ru.p3tr0vich.fuel.factories.FragmentFactory

class FragmentHelper(private val mFragmentActivity: FragmentActivity) {

    val currentFragment: FragmentInterface
        get() {
            return (mFragmentActivity.supportFragmentManager
                    .findFragmentById(R.id.content_frame) as FragmentInterface?)!!
        }

    val fragmentFueling: FragmentFueling?
        get() = getFragment(FragmentFactory.Ids.FUELING) as FragmentFueling?

    val fragmentCalc: FragmentCalc?
        get() = getFragment(FragmentFactory.Ids.CALC) as FragmentCalc?

    val fragmentPreferences: FragmentPreferences?
        get() = getFragment(FragmentFactory.Ids.PREFERENCES) as FragmentPreferences?

    fun getFragment(@FragmentFactory.Ids.Id fragmentId: Int): Fragment? {
        return mFragmentActivity.supportFragmentManager.findFragmentByTag(
                FragmentFactory.fragmentIdToTag(fragmentId))
    }

    fun addMainFragment() {
        mFragmentActivity.supportFragmentManager.beginTransaction()
                .add(R.id.content_frame,
                        FragmentFactory.getFragmentNewInstance(FragmentFactory.Ids.MAIN),
                        FragmentFactory.fragmentIdToTag(FragmentFactory.Ids.MAIN))
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .commit()
    }

    fun replaceFragment(@FragmentFactory.Ids.Id fragmentId: Int, args: Bundle?) {
        mFragmentActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame,
                        FragmentFactory.getFragmentNewInstance(fragmentId, args),
                        FragmentFactory.fragmentIdToTag(fragmentId))
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit()
    }
}