package com.alvexcore.repo.foldersize;

/**
 * Created by striker on 7/4/17.
 */
import java.io.IOException;
import java.util.*;

import org.json.JSONArray;
import org.springframework.extensions.webscripts.*;
import org.json.JSONException;
import org.json.JSONObject;


public class JsonDir extends AbstractWebScript
{

    private FolderSizeService folderSizeService;

    public void setFolderSizeService(FolderSizeService folderSizeService) {
        this.folderSizeService = folderSizeService;
    }

    public void execute(WebScriptRequest req, WebScriptResponse res)
            throws IOException
    {

        try
        {

            List<Map> total_list = folderSizeService.getFolderChildren(req);

            JSONObject obj = new JSONObject();
            JSONArray array = new JSONArray();

            for (Map map: total_list) {
                JSONObject child = new JSONObject();
                child.put(String.valueOf(map.get("propertyName")), map.get("size"));
                array.put(child);
            }

            String folderName = folderSizeService.getFolderName(req);

            obj.put("folder", folderName);
            obj.put("children", array);

            String jsonString = obj.toString(4);
            res.getWriter().write(jsonString);
        }
        catch(JSONException e)
        {
            throw new WebScriptException("Unable to serialize JSON");
        }
    }



}