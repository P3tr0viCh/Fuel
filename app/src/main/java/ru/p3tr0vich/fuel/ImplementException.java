package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Arrays;

class ImplementException  extends ClassCastException {

    ImplementException(@NonNull Context context, @NonNull Class[] ints) {
        super(context.getClass().getSimpleName() + " must implement " + Arrays.toString(ints));
    }

    ImplementException(@NonNull Context context, @NonNull Class cls) {
        this(context, new Class[]{cls});
    }
}