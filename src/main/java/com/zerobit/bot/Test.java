package com.zerobit.bot;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Test extends ListenerAdapter {

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
         String storedMessage = event.getMessage().getContentRaw();
         String storedUsername = event.getMember().getUser().getName();

         if(storedMessage.equalsIgnoreCase("Test123")) {
             event.getChannel().sendMessage("Hello " + storedUsername).queue();
         }
         if(storedMessage.equalsIgnoreCase("/ping")) {
             int delay = 100000;
             for(int i = 0; i < delay; i++) {
                 System.out.println("ping " + i);
             }
             event.getChannel().sendMessage("pong").queue();
         }

         if(storedMessage.equalsIgnoreCase("/stop")) {
             System.out.println("[Console] Discord Bot deactivated");
             System.exit(0);
         }


    }
}
