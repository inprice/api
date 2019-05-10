package io.inprice.scrapper.api.helpers;

import com.google.gson.Gson;

public class Global {

    public static final Gson gson = new Gson();

    public static volatile boolean isRunning = false;

}
