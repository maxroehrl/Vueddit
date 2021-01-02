package de.max.roehrl.vueddit2.model

import de.max.roehrl.vueddit2.service.Util
import org.json.JSONObject

class Video(json: JSONObject) {
    var src: String = ""
    var width: Int = 0
    var height: Int = 0

    init {
        when {
            json.optJSONObject("secure_media")?.optJSONObject("reddit_video") != null ->
                this.getVideoObject(json.optJSONObject("secure_media")?.optJSONObject("reddit_video"), "hls_url")

            json.optJSONObject("preview")?.optJSONObject("reddit_video_preview") != null ->
                this.getVideoObject(json.optJSONObject("preview")?.optJSONObject("reddit_video_preview"), "hls_url")

            json.optJSONObject("preview")?.optJSONArray("images")?.optJSONObject(0)?.optJSONObject("variants")?.optJSONObject("mp4")?.optJSONObject("source") != null ->
                this.getVideoObject(json.optJSONObject("preview")?.optJSONArray("images")?.optJSONObject(0)?.optJSONObject("variants")?.optJSONObject("mp4")?.optJSONObject("source"), "url")

            json.optJSONObject("secure_media_embed")?.optString("media_domain_url") != null ->
                this.getVideoObject(json.optJSONObject("secure_media_embed"), "media_domain_url")

        }
    }

    private fun getVideoObject(source: JSONObject?,
                       srcProp: String="src",
                       heightProp: String="height",
                       widthProp: String="width") {
        if (source != null) {
            src = source.optString(srcProp)
            if (listOf("hls_url", "url2").contains(srcProp)) {
                val type = if (srcProp == "hls_url") "application/x-mpegURL" else "video/mp4"
                src = this.getRedditDashVideoPlayerHtml(src, type)
            }
            height = source.optInt(heightProp, 0)
            width = source.optInt(widthProp, 0)
        }
    }

    private fun getRedditDashVideoPlayerHtml(src: String, type: String) : String {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>Video Player</title>
            <style>
                body {
                    margin: 0;
                    background-color: black;
                }
                .vjs-default-skin .vjs-control-bar {
                    background-color: rgba(62, 62, 62, 0.5) !important;
                }
                .vjs-default-skin .vjs-progress-control .vjs-load-progress > div {
                    background: rgba(0, 0, 0, 0.2);
                }
            </style>
            <link href="https://unpkg.com/video.js/dist/video-js.min.css" rel="stylesheet">
        </head>
        <body>
            <video-js id="vid1" class="video-js vjs-default-skin" muted autoplay controls loop width="${Util.getScreenWidth()}px">
                <source src="$src" type="$type" />
            </video-js>
            <script src="https://unpkg.com/video.js/dist/video.min.js"></script>
            <script src="https://unpkg.com/@videojs/http-streaming/dist/videojs-http-streaming.min.js"></script>
        <script>
            const player = videojs('vid1');
        </script>
        </body>
        </html>
        """.trimIndent()
    }
}