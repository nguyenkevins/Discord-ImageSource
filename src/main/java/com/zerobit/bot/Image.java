package com.zerobit.bot;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.io.File;
import java.nio.file.Paths;

public class Image extends ListenerAdapter {

    String pathToImage = "/image";

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        String storedMessage = event.getMessage().getContentRaw();
        String[] storedMessageArray = event.getMessage().getContentRaw().split(" ");
        String storedUsername = event.getMember().getUser().getName();

        if(storedMessage.equalsIgnoreCase("/image")) {
            File location = new File(pathToImage);
            try {
                if(!location.isDirectory()) {
                    throw new IllegalArgumentException("Illegal Arguement Exception");
                }
            }
            catch(IllegalArgumentException e) {
                System.out.println("WARNING! File directory not found!");
                System.exit(0);
            }
            File[] imageArray = location.listFiles();
            int randomNumber = (int)(Math.random()*imageArray.length);
            event.getChannel().sendFile(new File(imageArray[randomNumber].getAbsolutePath())).queue();
            System.out.println("[Console] "+storedUsername+" used /image");
            //event.getChannel().sendMessage("Hi " + storedUsername).queue();
        }
    }
}
