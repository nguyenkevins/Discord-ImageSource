/*
 * Copyright 2015-2019 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.internal.handle;

import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EmoteImpl;
import net.dv8tion.jda.internal.requests.WebSocketClient;
import net.dv8tion.jda.internal.utils.JDALogger;

public class MessageReactionHandler extends SocketHandler
{

    private final boolean add;

    public MessageReactionHandler(JDAImpl api, boolean add)
    {
        super(api);
        this.add = add;
    }

    @Override
    protected Long handleInternally(DataObject content)
    {
        if (!content.isNull("guild_id"))
        {
            long guildId = content.getLong("guild_id");
            if (getJDA().getGuildSetupController().isLocked(guildId))
                return guildId;
        }

        DataObject emoji = content.getObject("emoji");

        final long userId    = content.getLong("user_id");
        final long messageId = content.getLong("message_id");
        final long channelId = content.getLong("channel_id");

        final Long emojiId = emoji.isNull("id") ? null : emoji.getLong("id");
        String emojiName = emoji.getString("name", null);
        final boolean emojiAnimated = emoji.getBoolean("animated");

        if (emojiId == null && emojiName == null)
        {
            WebSocketClient.LOG.debug("Received a reaction {} with no name nor id. json: {}",
                JDALogger.getLazyString(() -> add ? "add" : "remove"), content);
            return null;
        }

        User user = getJDA().getUserById(userId);
        if (user == null)
            user = getJDA().getFakeUserMap().get(userId);
        if (user == null)
        {
            if (!add)
            {
                //This can be caused by a ban, we should just drop it in that case
                return null;
            }
            getJDA().getEventCache().cache(EventCache.Type.USER, userId, responseNumber, allContent, this::handle);
            EventCache.LOG.debug("Received a reaction for a user that JDA does not currently have cached. " +
                                 "UserID: {} ChannelId: {} MessageId: {}", userId, channelId, messageId);
            return null;
        }

        MessageChannel channel = getJDA().getTextChannelById(channelId);
        if (channel == null)
        {
            channel = getJDA().getPrivateChannelById(channelId);
        }
        if (channel == null)
        {
            channel = getJDA().getFakePrivateChannelMap().get(channelId);
        }
        if (channel == null)
        {
            getJDA().getEventCache().cache(EventCache.Type.CHANNEL, channelId, responseNumber, allContent, this::handle);
            EventCache.LOG.debug("Received a reaction for a channel that JDA does not currently have cached");
            return null;
        }

        MessageReaction.ReactionEmote rEmote;
        if (emojiId != null)
        {
            Emote emote = getJDA().getEmoteById(emojiId);
            if (emote == null)
            {
                if (emojiName != null)
                {
                    emote = new EmoteImpl(emojiId, getJDA()).setAnimated(emojiAnimated).setName(emojiName);
                }
                else
                {
                    WebSocketClient.LOG.debug("Received a reaction {} with a null name. json: {}",
                        JDALogger.getLazyString(() -> add ? "add" : "remove"), content);
                    return null;
                }
            }
            rEmote = MessageReaction.ReactionEmote.fromCustom(emote);
        }
        else
        {
            rEmote = MessageReaction.ReactionEmote.fromUnicode(emojiName, getJDA());
        }
        MessageReaction reaction = new MessageReaction(channel, rEmote, messageId, user.equals(getJDA().getSelfUser()), -1);

        if (add)
            onAdd(reaction, user);
        else
            onRemove(reaction, user);
        return null;
    }

    private void onAdd(MessageReaction reaction, User user)
    {
        JDAImpl jda = getJDA();
        switch (reaction.getChannelType())
        {
            case TEXT:
                jda.handleEvent(
                    new GuildMessageReactionAddEvent(
                        jda, responseNumber,
                        user, reaction));
                break;
            case PRIVATE:
                jda.handleEvent(
                    new PrivateMessageReactionAddEvent(
                        jda, responseNumber,
                        user, reaction));
                break;
            case GROUP:
                WebSocketClient.LOG.debug("Received a reaction add for a group which should not be possible");
                return;
        }

        jda.handleEvent(
            new MessageReactionAddEvent(
                jda, responseNumber,
                user, reaction));
    }

    private void onRemove(MessageReaction reaction, User user)
    {
        JDAImpl jda = getJDA();
        switch (reaction.getChannelType())
        {
            case TEXT:
                jda.handleEvent(
                    new GuildMessageReactionRemoveEvent(
                        jda, responseNumber,
                        user, reaction));
                break;
            case PRIVATE:
                jda.handleEvent(
                    new PrivateMessageReactionRemoveEvent(
                        jda, responseNumber,
                        user, reaction));
                break;
            case GROUP:
                WebSocketClient.LOG.debug("Received a reaction remove for a group which should not be possible");
                return;
        }

        jda.handleEvent(
            new MessageReactionRemoveEvent(
                jda, responseNumber,
                user, reaction));
    }
}
