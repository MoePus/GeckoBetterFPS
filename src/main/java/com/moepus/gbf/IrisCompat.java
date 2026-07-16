package com.moepus.gbf;

import net.minecraftforge.fml.loading.LoadingModList;

public class IrisCompat {
    private IrisCompat() {
    }

    public static final boolean IS_IRIS_INSTALLED = LoadingModList.get().getModFileById("iris") != null;
}
