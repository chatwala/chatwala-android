package com.chatwala.android;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/22/13
 * Time: 12:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatMessage
{
    public MessageMetadata metadata;
    public File messageVideo;

    //This comes from the file URI.  May be suspect, but if that's all we have, its a good guess
    public String probableEmailSource;
}
