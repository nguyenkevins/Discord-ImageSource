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

package net.dv8tion.jda.internal.entities;

import net.dv8tion.jda.api.entities.ActivityFlag;
import net.dv8tion.jda.api.entities.RichPresence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Objects;

public class RichPresenceImpl extends ActivityImpl implements RichPresence
{
    protected final long applicationId;

    protected final Party party;
    protected final String details;
    protected final String state;
    protected final Image largeImage;
    protected final Image smallImage;
    protected final String sessionId;
    protected final String syncId;
    protected final int flags;

    protected RichPresenceImpl(
        ActivityType type, String name, String url, long applicationId,
        Party party, String details, String state, Timestamps timestamps, String syncId, String sessionId, int flags,
        String largeImageKey, String largeImageText, String smallImageKey, String smallImageText)
    {
        super(name, url, type, timestamps);
        this.applicationId = applicationId;
        this.party = party;
        this.details = details;
        this.state = state;
        this.sessionId = sessionId;
        this.syncId = syncId;
        this.flags = flags;
        this.largeImage = largeImageKey != null ? new Image(applicationId, largeImageKey, largeImageText) : null;
        this.smallImage = smallImageKey != null ? new Image(applicationId, smallImageKey, smallImageText) : null;
    }

    @Override
    public boolean isRich()
    {
        return true;
    }

    @Override
    public RichPresence asRichPresence()
    {
        return this;
    }

    /**
     * The ID for the responsible application.
     *
     * @return The ID for the application
     */
    @Override
    public long getApplicationIdLong()
    {
        return applicationId;
    }

    /**
     * The ID for the responsible application.
     *
     * @return The ID for the application
     */
    @Nonnull
    @Override
    public String getApplicationId()
    {
        return Long.toUnsignedString(applicationId);
    }

    /**
     * Session ID for this presence.
     * <br>Used by spotify integration.
     *
     * @return Session ID
     */
    @Nullable
    @Override
    public String getSessionId()
    {
        return sessionId;
    }

    /**
     * Sync ID for this presence.
     * <br>Used by spotify integration.
     *
     * @return Sync ID
     */
    @Nullable
    @Override
    public String getSyncId()
    {
        return syncId;
    }

    /**
     * Flags for this presence
     *
     * @return The flags for this presence
     *
     * @see    ActivityFlag
     * @see    ActivityFlag#getFlags(int)
     */
    @Override
    public int getFlags()
    {
        return flags;
    }

    /**
     * Flags for this presence in an enum set
     *
     * @return The flags for this presence
     *
     * @see    ActivityFlag
     * @see    ActivityFlag#getFlags(int)
     */
    @Override
    public EnumSet<ActivityFlag> getFlagSet()
    {
        return ActivityFlag.getFlags(getFlags());
    }

    /**
     * The user's current party status
     * <br>Example: "Looking to Play", "Playing Solo", "In a Group"
     *
     * @return The user's current party status
     */
    @Nullable
    @Override
    public String getState()
    {
        return state;
    }

    /**
     * What the player is currently doing
     * <br>Example: "Competitive - Captain's Mode", "In Queue", "Unranked PvP"
     *
     * @return What the player is currently doing
     */
    @Nullable
    @Override
    public String getDetails()
    {
        return details;
    }

    /**
     * Information on the active party of the player
     *
     * @return {@link net.dv8tion.jda.api.entities.RichPresence.Party Party} wrapper or {@code null} if unset
     */
    @Nullable
    @Override
    public Party getParty()
    {
        return party;
    }

    /**
     * Information on the large image displayed in the profile view
     *
     * @return {@link net.dv8tion.jda.api.entities.RichPresence.Image Image} wrapper or {@code null} if unset
     */
    @Nullable
    @Override
    public Image getLargeImage()
    {
        return largeImage;
    }

    /**
     * Information on the small corner image displayed in the profile view
     *
     * @return {@link net.dv8tion.jda.api.entities.RichPresence.Image Image} wrapper or {@code null} if unset
     */
    @Nullable
    @Override
    public Image getSmallImage()
    {
        return smallImage;
    }

    @Override
    public String toString()
    {
        return String.format("RichPresence(%s / %s)", name, getApplicationId());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(applicationId, state, details, party, sessionId, syncId, flags, timestamps, largeImage, smallImage);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof RichPresenceImpl))
            return false;
        RichPresenceImpl p = (RichPresenceImpl) o;
        return applicationId == p.applicationId
               && Objects.equals(name, p.name)
               && Objects.equals(url, p.url)
               && Objects.equals(type, p.type)
               && Objects.equals(state, p.state)
               && Objects.equals(details, p.details)
               && Objects.equals(party, p.party)
               && Objects.equals(sessionId, p.sessionId)
               && Objects.equals(syncId, p.syncId)
               && Objects.equals(flags, p.flags)
               && Objects.equals(timestamps, p.timestamps)
               && Objects.equals(largeImage, p.largeImage)
               && Objects.equals(smallImage, p.smallImage);
    }
}
