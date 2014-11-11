package io.lumify.web;

import io.lumify.miniweb.handlers.AppendableStaticResourceHandler;

public class No404AppendableStaticResourceHandler extends AppendableStaticResourceHandler {
    public No404AppendableStaticResourceHandler(String contentType) {
        super(contentType);
        super.appendResource("/" + this.getClass().getName().replace(".", "/") + ".txt");
    }
}
