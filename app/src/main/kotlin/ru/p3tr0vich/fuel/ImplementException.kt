package ru.p3tr0vich.fuel

import android.content.Context

internal class ImplementException(context: Context, ints: Array<Class<*>>) :
        ClassCastException(context.javaClass.simpleName + " must implement " + ints.contentToString()) {
    constructor(context: Context, cls: Class<*>) : this(context, arrayOf<Class<*>>(cls))
}