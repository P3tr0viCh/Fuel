package ru.p3tr0vich.fuel

import android.content.Context
import java.util.*

internal class ImplementException(context: Context, ints: Array<Class<*>>) :
        ClassCastException(context.javaClass.simpleName + " must implement " + Arrays.toString(ints)) {
    constructor(context: Context, cls: Class<*>) : this(context, arrayOf<Class<*>>(cls))
}