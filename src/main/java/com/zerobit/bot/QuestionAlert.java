package com.zerobit.bot;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class QuestionAlert extends ListenerAdapter {
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        String[] storedMessageArray = event.getMessage().getContentRaw().split(" ");
        if (storedMessageArray[0].equalsIgnoreCase("what") || storedMessageArray[0].equalsIgnoreCase("how") ||
            storedMessageArray[0].equalsIgnoreCase("why") || storedMessageArray[0].equalsIgnoreCase("when") ||
            storedMessageArray[0].equalsIgnoreCase("which")) {
            event.getChannel().sendMessage("Sounds like a good question").queue();
        }
    }
}





