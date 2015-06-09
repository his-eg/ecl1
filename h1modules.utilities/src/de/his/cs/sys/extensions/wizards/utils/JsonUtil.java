package de.his.cs.sys.extensions.wizards.utils;

import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;

public class JsonUtil {

    public static <T> T fromJson(Class<T> clazz, InputStream source) {
        Gson g = new Gson();
        InputStreamReader reader = new InputStreamReader(source);
        T t = g.fromJson(reader, clazz);
        return t;
    }

}
