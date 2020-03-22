package io.inprice.scrapper.api.consts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Global {

   public static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

   public static volatile boolean isApplicationRunning = false;

}
