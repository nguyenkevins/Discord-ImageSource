package com.zerobit.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class BotMain {
    public static void main(String[] args) throws Exception{

        JDA jda = new JDABuilder("NTMyNjQ5MTI2MDk0MTc2MjU2.XXvunA.LYKzFaCRTrnEisy4UTlGZVU3N2A").build();

        jda.addEventListener(new Test());
        jda.addEventListener(new Image());
        jda.addEventListener(new QuestionAlert());
        System.out.println("[Console] ------------------------------");
        System.out.println("[Console] Discord Bot Activated");
    }
}
