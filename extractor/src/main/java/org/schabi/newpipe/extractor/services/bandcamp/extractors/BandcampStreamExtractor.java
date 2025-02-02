// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

package org.schabi.newpipe.extractor.services.bandcamp.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.MetaInfo;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.*;
import org.schabi.newpipe.extractor.utils.JsonUtils;
import org.schabi.newpipe.extractor.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampExtractorHelper.getImageUrl;

public class BandcampStreamExtractor extends StreamExtractor {

    private JsonObject albumJson;
    private JsonObject current;
    private Document document;

    public BandcampStreamExtractor(final StreamingService service, final LinkHandler linkHandler) {
        super(service, linkHandler);
    }


    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws IOException, ExtractionException {
        final String html = downloader.get(getLinkHandler().getUrl()).responseBody();
        document = Jsoup.parse(html);
        albumJson = getAlbumInfoJson(html);
        current = albumJson.getObject("current");

        if (albumJson.getArray("trackinfo").size() > 1) {
            // In this case, we are actually viewing an album page!
            throw new ExtractionException("Page is actually an album, not a track");
        }
    }

    /**
     * Get the JSON that contains album's metadata from page
     *
     * @param html Website
     * @return Album metadata JSON
     * @throws ParsingException In case of a faulty website
     */
    public static JsonObject getAlbumInfoJson(final String html) throws ParsingException {
        try {
            return JsonUtils.getJsonData(html, "data-tralbum");
        } catch (final JsonParserException e) {
            throw new ParsingException("Faulty JSON; page likely does not contain album data", e);
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ParsingException("JSON does not exist", e);
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return current.getString("title");
    }

    @Nonnull
    @Override
    public String getUploaderUrl() throws ParsingException {
        final String[] parts = getUrl().split("/");
        // https: (/) (/) * .bandcamp.com (/) and leave out the rest
        return "https://" + parts[2] + "/";
    }

    @Nonnull
    @Override
    public String getUrl() throws ParsingException {
        return albumJson.getString("url").replace("http://", "https://");
    }

    @Nonnull
    @Override
    public String getUploaderName() {
        return albumJson.getString("artist");
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return current.getString("publish_date");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return BandcampExtractorHelper.parseDate(getTextualUploadDate());
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() throws ParsingException {
        if (albumJson.isNull("art_id")) return "";
        else return getImageUrl(albumJson.getLong("art_id"), true);
    }

    @Nonnull
    @Override
    public String getUploaderAvatarUrl() {
        try {
            return document.getElementsByClass("band-photo").first().attr("src");
        } catch (final NullPointerException e) {
            return "";
        }
    }

    @Nonnull
    @Override
    public String getSubChannelUrl() {
        return "";
    }

    @Nonnull
    @Override
    public String getSubChannelName() {
        return "";
    }

    @Nonnull
    @Override
    public String getSubChannelAvatarUrl() {
        return "";
    }

    @Nonnull
    @Override
    public Description getDescription() {
        final String s = Utils.nonEmptyAndNullJoin(
                "\n\n",
                new String[]{
                        current.getString("about"),
                        current.getString("lyrics"),
                        current.getString("credits")
                }
        );
        return new Description(s, Description.PLAIN_TEXT);
    }

    @Override
    public int getAgeLimit() {
        return NO_AGE_LIMIT;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public long getViewCount() {
        return -1;
    }

    @Override
    public long getLikeCount() {
        return -1;
    }

    @Override
    public long getDislikeCount() {
        return -1;
    }

    @Nonnull
    @Override
    public String getDashMpdUrl() {
        return "";
    }

    @Nonnull
    @Override
    public String getHlsUrl() {
        return "";
    }

    @Override
    public List<AudioStream> getAudioStreams() {
        final List<AudioStream> audioStreams = new ArrayList<>();

        audioStreams.add(new AudioStream(
                albumJson.getArray("trackinfo").getObject(0)
                        .getObject("file").getString("mp3-128"),
                MediaFormat.MP3, 128
        ));
        return audioStreams;
    }

    @Override
    public List<VideoStream> getVideoStreams() {
        return Collections.emptyList();
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<SubtitlesStream> getSubtitlesDefault() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<SubtitlesStream> getSubtitles(MediaFormat format) {
        return Collections.emptyList();
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.AUDIO_STREAM;
    }

    @Override
    public StreamInfoItemsCollector getRelatedStreams() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Nonnull
    @Override
    public String getHost() {
        return "";
    }

    @Nonnull
    @Override
    public Privacy getPrivacy() {
        return Privacy.PUBLIC;
    }

    @Nonnull
    @Override
    public String getCategory() {
        // Get first tag from html, which is the artist's Genre
        return document
                .getElementsByClass("tralbum-tags").first()
                .getElementsByClass("tag").first().text();
    }

    @Nonnull
    @Override
    public String getLicence() {

        int license = current.getInt("license_type");

        // Tests resulted in this mapping of ints to licence: https://cloud.disroot.org/s/ZTWBxbQ9fKRmRWJ/preview

        switch (license) {
            case 1:
                return "All rights reserved ©";
            case 2:
                return "CC BY-NC-ND 3.0";
            case 3:
                return "CC BY-NC-SA 3.0";
            case 4:
                return "CC BY-NC 3.0";
            case 5:
                return "CC BY-ND 3.0";
            case 6:
                return "CC BY 3.0";
            case 8:
                return "CC BY-SA 3.0";
            default:
                return "Unknown";
        }
    }

    @Nullable
    @Override
    public Locale getLanguageInfo() {
        return null;
    }

    @Nonnull
    @Override
    public List<String> getTags() {
        final Elements tagElements = document.getElementsByAttributeValue("itemprop", "keywords");

        final List<String> tags = new ArrayList<>();

        for (final Element e : tagElements) {
            tags.add(e.text());
        }

        return tags;
    }

    @Nonnull
    @Override
    public String getSupportInfo() {
        return "";
    }

    @Nonnull
    @Override
    public List<StreamSegment> getStreamSegments() throws ParsingException {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() throws ParsingException {
        return Collections.emptyList();
    }
}
