package com.sadiq.megatransfer;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by sadiq7753 on 5/24/2015.
 */
public interface BoundServiceListener {

    public void receiveSocketMessage(String type, String body);

    public void receiveSocketArray(String type, JSONObject body);

}
