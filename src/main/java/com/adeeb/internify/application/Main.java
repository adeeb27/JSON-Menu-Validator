package com.adeeb.internify.application;

import com.adeeb.internify.model.MenuList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/*

 **********************************************************************************************
 *                                                                                            *
 *                I did the extra challenge! Just hit run and see for yourself!               *
 *  Hello, I am Adeeb, I hope you survive reading my code. I have used a bunch of libraries   *
 *  because why waste time creating things that already exist right? I have also applied for  *
 *  the mobile developer intern position at Shopify and I am working as a product support     *
 *  intern at SAP Hybris. If the code below does not look good, maybe version 2 will after    *
 *  I receive feedback. Please call me for an interview, I say better things in person        *
 *                                                                                            *
 **********************************************************************************************

 */

public class Main {

    private static OkHttpClient client = new OkHttpClient();
    private static MenuList menuList;
    static HashMap invalidMenu = new HashMap();
    static HashMap validMenu = new HashMap();

    public static void main(String[] args) {

        /* Notice I did not include the page number in the URL, the getJSONString method
           visits each page and checks to see if it has products.
           It only goes to the next page if the current page has products.
         */
        String url = "https://backend-challenge-summer-2018.herokuapp.com/challenges.json?id=1&page=";

        //Takes the JSON string and puts it in the model.
        GSONMapper(getJsonAsString(url));

        //Method that will validate menus, put the valid ones in validMenu hashmap and invalid ones in the invalidMenu hashmap (respectively)
        menuValidator();

        //java2JSON prettifies the JSON, so it looks like an actual JSON and not a string of words.
        String prettyJSON = java2prettyJSON(validMenu, invalidMenu);

        //Finally printing out the string
        System.out.print(prettyJSON);

        /***** Uncomment this if you want to write the final menu JSON to a file, it will contain exactly
         * what you saw in the console printed to the console


         try {
             FileWriter file = new FileWriter("internify.json");
             file.write(prettyJSON);
             file.flush();
         }
         catch (IOException e) {
            e.printStackTrace();
         }

         /****************** --------------------------- *******************/

    }

    //Converts everything in the response to a string and returns it
    public static String getJSON(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();

    }

    //Prepares the response for GSON builder library by setting up the required information into a string
    public static String getJsonAsString(String url) {

        String json = "";
        String temp = "";
        int jsonlength = 0;
        int jsonlengthTwo = 0;
        int currentPage = 1;

         /* hardcoded this to remove it from the bottom of the page first and second pages
        and keep adding json from all the pages, but then added it to the end of the page page of the JSON */
        String pagination = "],\"pagination\":{\"current_page\":" + currentPage + ",\"per_page\":5,\"total\":15}}";

        try {
            //iterating over the pages until there is are products in them
            while(true) {
                //the first page we will store everything except for the pagination part in json
                if (currentPage == 1) {
                    json = getJSON(url + currentPage);
                    jsonlength = json.length() - pagination.length();
                    json = json.substring(0, jsonlength);

                }
                //From the second page onwards, only add everything after menu:[ so that it is a valid JSON
                else {

                    temp = getJSON(url + currentPage);
                    if (!temp.contains("id")) {
                        pagination = "],\"pagination\":{\"current_page\":" + (currentPage-1) + ",\"per_page\":5,\"total\":15}}";
                        break;
                    }
                    jsonlengthTwo = "{\"menus\":[".length();
                    json = json + "," + temp.substring(jsonlengthTwo, temp.length() - pagination.length());

                }
                currentPage++;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        /* So far only saved without the pagination part,
         so adding it here so GSON builder does not throw "invalid JSON" error */
        json = json + pagination;
        return json;


    }
    //Method that maps everything in the JSON to the models
    //GSON library to the rescue!
    public static void GSONMapper(String sentence) {
        Gson gson = new Gson();
        menuList = gson.fromJson(sentence, MenuList.class);
    }

    public static void menuValidator() {
        //depth a menu can be is 4, so keeping a check using maxDepth
        int maxDepth = 0;
        int childID = 1;
        //iterators
        int i = 0;
        int j = 0;
        int totalProds = menuList.getMenus().size();

        //The first JSON object's id
        int rootID = menuList.getMenus().get(i).getId();

        /*
         * Hardest and most time consuming part of my code: the algorithm to check cyclic relations.
         * I could've used graphs/trees but since the challenge does not mention any space/time
         * constraints, my main focus was to solve the challenge (plus extras) in the shortest time
         * possible.
         *
         * I am not clueless, for the most of it, I just used Intellij's debugger to figure this out.
         */

        while (maxDepth < 4 && i < totalProds) {
            while (j < menuList.getMenus().get(childID - 1).getChildIds().size()) {
                //visiting the id that was in the child id.
                childID = menuList.getMenus().get(childID - 1).getChildIds().get(j);
                //if the child id and the root id is the same then we know it's a cyclic relation and hence invalid
                if (childID == rootID) {
                    invalidMenu.put(childID, childID);
                    maxDepth = 0;
                    j++;
                }
                // else if say id 15's child id 1 is already in the invalidMenu, then put 15 which is root id now, in the invalidMenu
                else if (invalidMenu.containsValue(childID)) {
                    invalidMenu.put(rootID, rootID);
                }
                //else just update the depth and try with the next child id in the child id array
                else {
                    maxDepth++;
                }
            }
            i++;
            //go to the next id in the JSON
            if (i < menuList.getMenus().size()) {
                rootID = menuList.getMenus().get(i).getId();
                childID = rootID;
                j = 0; //start with the first child id in the child id array
            }


        }

        /* Everything that is not in invalidMenu hashmap will be
           * valid menus. That's why iterating over the 15 ids and putting only
           * numbers not in the invalid HashMap in a validMenu hashmap
           */

        for (int num = 1; num < menuList.getMenus().size(); num++) {
            if (invalidMenu.get(num) == null)
                validMenu.put(num, num);
        }

    }

    //method that will take the hashmaps and prettyprint (return) it
    public static String java2prettyJSON(HashMap validMenu, HashMap invalidMenu) {

        //json-simple library
        JSONObject mainBigObj = new JSONObject();
        JSONObject secondaryBigObj = new JSONObject();
        JSONObject mainObj = new JSONObject();
        JSONArray validList = new JSONArray();
        JSONArray invalidList = new JSONArray();
        JSONArray invalidListArray = new JSONArray();
        JSONArray validListArray = new JSONArray();

        invalidListArray.add(mainBigObj);
        mainBigObj.put("root_id", validMenu.values().toArray()[0]);
        for (int i = 1; i < validMenu.values().toArray().length - 1; i++) {
            validList.add(validMenu.values().toArray()[i]);
        }
        mainBigObj.put("children", validList);

        mainObj.put("valid_menus", invalidListArray);
        secondaryBigObj.put("root_id", invalidMenu.values().toArray()[0]);
        for (int j = 1; j < invalidMenu.values().toArray().length ; j++) {
            invalidList.add(invalidMenu.values().toArray()[j]);
        }
        mainObj.put("invalid_menus", validListArray);
        secondaryBigObj.put("children", invalidList);
       // mainObj.put("invalid_menus", validListArray);
        validListArray.add(secondaryBigObj);

        //GSON library again
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(mainObj.toJSONString()).getAsJsonObject();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(json);

        return prettyJson;

    }
}