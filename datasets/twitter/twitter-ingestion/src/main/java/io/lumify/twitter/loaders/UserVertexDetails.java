package io.lumify.twitter.loaders;

import static com.google.common.base.Preconditions.checkNotNull;
import net.jcip.annotations.Immutable;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * Simple POJO used to represent the required details necessary for the {@link UserVertexLoader}
 */
@Immutable
public final class UserVertexDetails {
    private final long id;
    private final String name;
    private final String screenName;
    private final String profileImageUrl;


    private UserVertexDetails(final long userId, final String displayName,
                              final String handle, final String imageUrl) {
        id = userId;
        name = displayName;
        screenName = handle;
        profileImageUrl = imageUrl;
    }

    /**
     * Converts the provided status to a details instance.  The provided status must contain {@link User} information.
     * @param status The status to convert, not null
     * @return A details instance comprised of the provided information
     */
    public static UserVertexDetails fromTweetStatus(final Status status) {
        checkNotNull(status);
        final User userData = checkNotNull(status.getUser());

        return new UserVertexDetails(userData.getId(), userData.getName(), userData.getScreenName(), userData.getProfileImageURL());
    }

    /**
     * Converts the provided user mention to a details instance
     * @param entity The user mention to convert, not null
     * @return A details instance comprised of the provided information
     */
    public static UserVertexDetails fromUserMention(final UserMentionEntity entity) {
        checkNotNull(entity);

        return new UserVertexDetails(entity.getId(), entity.getName(), entity.getScreenName(), "");
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getScreenName() {
        return screenName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }
}
