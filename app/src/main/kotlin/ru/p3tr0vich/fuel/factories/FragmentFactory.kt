package ru.p3tr0vich.fuel.factories

import android.os.Bundle
import androidx.annotation.IntDef
import androidx.fragment.app.Fragment
import ru.p3tr0vich.fuel.fragments.*

object FragmentFactory {

    interface Ids {

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(BAD_ID, FUELING, CALC, CHART_COST, BACKUP, PREFERENCES, ABOUT)
        annotation class Id

        companion object {
            const val BAD_ID = -1
            const val FUELING = 0
            const val CALC = 1
            const val CHART_COST = 2
            const val BACKUP = 3
            const val PREFERENCES = 4
            const val ABOUT = 5

            const val MAIN = FUELING
        }
    }

    @JvmStatic
    @JvmOverloads
    fun getFragmentNewInstance(@Ids.Id fragmentId: Int, args: Bundle? = null): Fragment {
        val fragment: Fragment = when (fragmentId) {
            Ids.CALC -> FragmentCalc()
            Ids.CHART_COST -> FragmentChartCost()
            Ids.PREFERENCES -> FragmentPreferences()
            Ids.BACKUP -> FragmentBackup()
            Ids.ABOUT -> FragmentAbout()
            Ids.FUELING -> FragmentFueling()
            Ids.BAD_ID -> throw IllegalArgumentException("Fragment bad ID")
            else -> throw IllegalArgumentException("Fragment bad ID")
        }

        fragment.arguments = args

        return fragment
    }

    @JvmStatic
    fun fragmentIdToTag(@Ids.Id id: Int): String {
        when (id) {
            Ids.ABOUT, Ids.BACKUP, Ids.CALC, Ids.CHART_COST, Ids.FUELING, Ids.PREFERENCES -> return "TAG_" + id.toString()
            Ids.BAD_ID -> throw IllegalArgumentException("Fragment bad ID")
            else -> throw IllegalArgumentException("Fragment bad ID")
        }
    }
}