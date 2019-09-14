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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildVoiceStateImpl;
import net.dv8tion.jda.internal.entities.MemberImpl;
import net.dv8tion.jda.internal.entities.VoiceChannelImpl;
import net.dv8tion.jda.internal.managers.AudioManagerImpl;

import java.util.Objects;

public class VoiceStateUpdateHandler extends SocketHandler
{
    public VoiceStateUpdateHandler(JDAImpl api)
    {
        super(api);
    }

    @Override
    protected Long handleInternally(DataObject content)
    {
        final Long guildId = content.isNull("guild_id") ? null : content.getLong("guild_id");
        if (guildId == null)
            return null; //unhandled for calls
        if (getJDA().getGuildSetupController().isLocked(guildId))
            return guildId;
        handleGuildVoiceState(content);
        return null;
    }

    private void handleGuildVoiceState(DataObject content)
    {
        final long userId = content.getLong("user_id");
        final long guildId = content.getLong("guild_id");
        final Long channelId = !content.isNull("channel_id") ? content.getLong("channel_id") : null;
        final String sessionId = !content.isNull("session_id") ? content.getString("session_id") : null;
        boolean selfMuted = content.getBoolean("self_mute");
        boolean selfDeafened = content.getBoolean("self_deaf");
        boolean guildMuted = content.getBoolean("mute");
        boolean guildDeafened = content.getBoolean("deaf");
        boolean suppressed = content.getBoolean("suppress");

        Guild guild = getJDA().getGuildById(guildId);
        if (guild == null)
        {
            getJDA().getEventCache().cache(EventCache.Type.GUILD, guildId, responseNumber, allContent, this::handle);
            EventCache.LOG.debug("Received a VOICE_STATE_UPDATE for a Guild that has yet to be cached. JSON: {}", content);
            return;
        }

        VoiceChannelImpl channel = channelId != null ? (VoiceChannelImpl) guild.getVoiceChannelById(channelId) : null;
        if (channel == null && channelId != null)
        {
            getJDA().getEventCache().cache(EventCache.Type.CHANNEL, channelId, responseNumber, allContent, this::handle);
            EventCache.LOG.debug("Received VOICE_STATE_UPDATE for a VoiceChannel that has yet to be cached. JSON: {}", content);
            return;
        }

        MemberImpl member = (MemberImpl) guild.getMemberById(userId);
        if (member == null)
        {
            //Caching of this might not be valid. It is possible that we received this
            // update due to this Member leaving the guild while still connected to a voice channel.
            // In that case, we should not cache this because it could cause problems if they rejoined.
            //However, we can't just ignore it completely because it could be a user that joined off of
            // an invite to a VoiceChannel, so the GUILD_MEMBER_ADD and the VOICE_STATE_UPDATE may have
            // come out of order. Not quite sure what to do. Going to cache for now however.
            //At the worst, this will just cause a few events to fire with bad data if the member rejoins the guild if
            // in fact the issue was that the VOICE_STATE_UPDATE was sent after they had left, however, by caching
            // it we will preserve the integrity of the cache in the event that it was actually a mis-ordering of
            // GUILD_MEMBER_ADD and VOICE_STATE_UPDATE. I'll take some bad-data events over an invalid cache.
            long idHash = guildId ^ userId;
            getJDA().getEventCache().cache(EventCache.Type.MEMBER, idHash, responseNumber, allContent, this::handle);
            EventCache.LOG.debug("Received VOICE_STATE_UPDATE for a Member that has yet to be cached. HASH_ID: {} JSON: {}", idHash, content);
            return;
        }

        GuildVoiceStateImpl vState = (GuildVoiceStateImpl) member.getVoiceState();
        if (vState == null)
            return;
        vState.setSessionId(sessionId); //Cant really see a reason for an event for this
        VoiceDispatchInterceptor voiceInterceptor = getJDA().getVoiceInterceptor();
        boolean isSelf = guild.getSelfMember().equals(member);

        boolean wasMute = vState.isMuted();
        boolean wasDeaf = vState.isDeafened();

        if (selfMuted != vState.isSelfMuted())
        {
            vState.setSelfMuted(selfMuted);
            getJDA().handleEvent(new GuildVoiceSelfMuteEvent(getJDA(), responseNumber, member));
        }
        if (selfDeafened != vState.isSelfDeafened())
        {
            vState.setSelfDeafened(selfDeafened);
            getJDA().handleEvent(new GuildVoiceSelfDeafenEvent(getJDA(), responseNumber, member));
        }
        if (guildMuted != vState.isGuildMuted())
        {
            vState.setGuildMuted(guildMuted);
            getJDA().handleEvent(new GuildVoiceGuildMuteEvent(getJDA(), responseNumber, member));
        }
        if (guildDeafened != vState.isGuildDeafened())
        {
            vState.setGuildDeafened(guildDeafened);
            getJDA().handleEvent(new GuildVoiceGuildDeafenEvent(getJDA(), responseNumber, member));
        }
        if (suppressed != vState.isSuppressed())
        {
            vState.setSuppressed(suppressed);
            getJDA().handleEvent(new GuildVoiceSuppressEvent(getJDA(), responseNumber, member));
        }
        if (wasMute != vState.isMuted())
            getJDA().handleEvent(new GuildVoiceMuteEvent(getJDA(), responseNumber, member));
        if (wasDeaf != vState.isDeafened())
            getJDA().handleEvent(new GuildVoiceDeafenEvent(getJDA(), responseNumber, member));
            
        if (!Objects.equals(channel, vState.getChannel()))
        {
            VoiceChannelImpl oldChannel = (VoiceChannelImpl) vState.getChannel();
            vState.setConnectedChannel(channel);

            if (oldChannel == null)
            {
                channel.getConnectedMembersMap().put(userId, member);
                getJDA().handleEvent(
                        new GuildVoiceJoinEvent(
                                getJDA(), responseNumber,
                                member));
            }
            else if (channel == null)
            {
                oldChannel.getConnectedMembersMap().remove(userId);
                if (isSelf)
                    getJDA().getDirectAudioController().update(guild, null);
                getJDA().handleEvent(
                        new GuildVoiceLeaveEvent(
                                getJDA(), responseNumber,
                                member, oldChannel));
            }
            else
            {
                AudioManagerImpl mng = (AudioManagerImpl) getJDA().getAudioManagersView().get(guildId);
                //If the currently connected account is the one that is being moved
                if (isSelf && mng != null && voiceInterceptor == null)
                {
                    //And this instance of JDA is connected or attempting to connect,
                    // then change the channel we expect to be connected to.
                    if (mng.isConnected() || mng.isAttemptingToConnect())
                        mng.setConnectedChannel(channel);

                    //If we have connected (VOICE_SERVER_UPDATE received and AudioConnection created (actual connection might still be setting up)),
                    // then we need to stop sending audioOpen/Move requests through the MainWS if the channel
                    // we have just joined / moved to is the same as the currently queued audioRequest
                    // (handled by updateAudioConnection)
                    if (mng.isConnected())
                        getJDA().getDirectAudioController().update(guild, channel);
                    //If we are not already connected this will be removed by VOICE_SERVER_UPDATE
                }

                channel.getConnectedMembersMap().put(userId, member);
                oldChannel.getConnectedMembersMap().remove(userId);
                getJDA().handleEvent(
                        new GuildVoiceMoveEvent(
                                getJDA(), responseNumber,
                                member, oldChannel));
            }
        }
        if (isSelf && voiceInterceptor != null)
        {
            if (voiceInterceptor.onVoiceStateUpdate(new VoiceDispatchInterceptor.VoiceStateUpdate(channel, vState, allContent)))
                getJDA().getDirectAudioController().update(guild, channel);
        }
    }
}
