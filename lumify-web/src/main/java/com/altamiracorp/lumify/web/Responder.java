package com.altamiracorp.lumify.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class Responder {
    public static void respondWith(final HttpServletResponse response, final JSONObject json) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.toString());
    }

    public static void respondWith(final HttpServletResponse response, final JSONArray json) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.toString());
    }

    public static void respondWith(final HttpServletResponse response, final String plainText) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(plainText);
    }

    private Responder() {

    }
}
