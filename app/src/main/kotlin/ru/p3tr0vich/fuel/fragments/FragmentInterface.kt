package ru.p3tr0vich.fuel.fragments

import android.support.annotation.StringRes

import ru.p3tr0vich.fuel.factories.FragmentFactory

interface FragmentInterface {

    @get:FragmentFactory.Ids.Id
    val fragmentId: Int

    @get:StringRes
    val titleId: Int?

    val title: String?

    @get:StringRes
    val subtitleId: Int

    val subtitle: String?

    interface OnFragmentChangeListener {
        fun onFragmentChange(fragment: FragmentInterface)
    }

    /**
     * Обрабатывает нажатие кнопки "назад".
     *
     * @return Истина, если нажатие было обработано во фрагменте,
     * ложь, если нажатие нужно обработать в активности.
     * По умолчанию ложь.
     * @see android.app.Activity.onBackPressed
     */
    fun onBackPressed(): Boolean
}