package com.altamiracorp.lumify.web;

public class Server {
    public static void main(String[] args) {
        p("==================================");
        p("RUN CONFIGURATION CHANGES REQUIRED");
        p("==================================");
        p("");
        p("Update your Jetty server run configuration and set the Main class to");
        p("com.altamiracorp.lumify.web.JettyWebServer class in the");
        p("lumify-jetty-server module.");
        p("");
        p("Then, copy you Jetty run configuration and name the new one Tomcat.");
        p("Change the Main Class to com.altamiracorp.lumify.web.TomcatWebServer");
        p("and the module to lumify-tomcat-server");
        p("");
        p("==================================");
    }

    private static void p(String line) {
        System.out.println(line);
    }
}
