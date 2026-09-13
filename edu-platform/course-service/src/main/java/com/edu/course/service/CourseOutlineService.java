package com.edu.course.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.edu.common.core.exception.BusinessException;
import com.edu.course.dto.CourseOutlineVO;
import com.edu.course.dto.CourseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseOutlineService {
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_ITEMS = 60;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; EduPlatformOutlineBot/1.0)";
    private static final Pattern BILIBILI_BVID = Pattern.compile("/video/(BV[0-9A-Za-z]+)", Pattern.CASE_INSENSITIVE);
    private static final List<String> OUTLINE_SELECTORS = List.of(
            "[itemprop=itemListElement] [itemprop=name]", "[itemprop=hasCourseInstance] [itemprop=name]",
            "[class*=curriculum] li", "[class*=syllabus] li", "[class*=outline] li",
            "[class*=chapter]", "[class*=lesson]", "[class*=section-title]",
            "[id*=curriculum] li", "[id*=syllabus] li", "[id*=outline] li"
    );

    private final CourseService courseService;
    private final ObjectMapper objectMapper;

    public CourseOutlineVO extract(Long courseId) {
        CourseVO course = courseService.getCourseById(courseId);
        String link = course.getLinkUrl();
        if (link == null || link.isBlank()) {
            return result(null, course.getTitle(), "unavailable", "管理员尚未配置课程链接", List.of());
        }
        try {
            CourseOutlineVO bilibili = extractBilibili(link.trim());
            if (bilibili != null && !bilibili.getItems().isEmpty()) return bilibili;
            FetchResult fetched = fetch(link.trim());
            List<String> items = parseOutline(fetched.document());
            if (items.isEmpty()) {
                return result(fetched.url(), fetched.document().title(), "empty",
                        "已访问课程链接，但页面没有可识别的公开目录", List.of());
            }
            return result(fetched.url(), fetched.document().title(), "success",
                    "已根据课程页面自动整理", items);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("课程目录解析失败, courseId={}, url={}, reason={}", courseId, link, e.getMessage());
            return result(link, course.getTitle(), "failed", "暂时无法读取课程页面，请稍后重试", List.of());
        }
    }

    private CourseOutlineVO extractBilibili(String url) throws Exception {
        Matcher matcher = BILIBILI_BVID.matcher(url);
        if (!matcher.find()) return null;
        String bvid = matcher.group(1);
        String apiUrl = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;
        validatePublicHttpUrl(apiUrl);
        String json = Jsoup.connect(apiUrl).userAgent(USER_AGENT).timeout(8000)
                .maxBodySize(2 * 1024 * 1024).ignoreContentType(true).execute().body();
        JsonNode data = objectMapper.readTree(json).path("data");
        if (data.isMissingNode() || data.isNull()) return null;

        LinkedHashSet<String> items = new LinkedHashSet<>();
        JsonNode seasonSections = data.path("ugc_season").path("sections");
        if (seasonSections.isArray()) {
            for (JsonNode section : seasonSections) {
                for (JsonNode episode : section.path("episodes")) addCandidate(items, episode.path("title").asText());
            }
        }
        if (items.size() < 2) {
            for (JsonNode page : data.path("pages")) addCandidate(items, page.path("part").asText());
        }
        String title = data.path("title").asText("哔哩哔哩课程");
        return result(url, title, items.isEmpty() ? "empty" : "success",
                items.isEmpty() ? "该视频没有公开的分P或合集目录" : "已根据视频分P与合集自动整理",
                items.stream().limit(MAX_ITEMS).toList());
    }

    private FetchResult fetch(String initialUrl) throws Exception {
        String current = initialUrl;
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            URI uri = validatePublicHttpUrl(current);
            Connection.Response response = Jsoup.connect(uri.toString())
                    .userAgent(USER_AGENT).timeout(8000).maxBodySize(2 * 1024 * 1024)
                    .followRedirects(false).ignoreHttpErrors(false).execute();
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = response.header("Location");
                if (location == null || location.isBlank() || i == MAX_REDIRECTS) {
                    throw new IllegalStateException("重定向地址无效");
                }
                current = uri.resolve(location).toString();
                continue;
            }
            String contentType = response.contentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("html")) {
                throw new IllegalStateException("课程链接不是 HTML 页面");
            }
            return new FetchResult(uri.toString(), response.parse());
        }
        throw new IllegalStateException("重定向次数过多");
    }

    private URI validatePublicHttpUrl(String value) throws Exception {
        URI uri = URI.create(value);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
            throw new BusinessException("课程链接必须是有效的 HTTP 或 HTTPS 公网地址");
        }
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new BusinessException("出于安全原因，不能解析本机或内网课程链接");
            }
        }
        return uri;
    }

    private List<String> parseOutline(Document document) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        for (String selector : OUTLINE_SELECTORS) {
            for (Element element : document.select(selector)) addCandidate(candidates, element.text());
        }
        if (candidates.size() < 2) {
            for (Element element : document.select("main h2, main h3, article h2, article h3, .content h2, .content h3")) {
                addCandidate(candidates, element.text());
            }
        }
        return candidates.stream().limit(MAX_ITEMS).toList();
    }

    private void addCandidate(Set<String> values, String raw) {
        if (raw == null) return;
        String value = raw.replaceAll("\\s+", " ").trim();
        value = value.replaceFirst("^(第\\s*\\d+\\s*[章节课]|chapter\\s*\\d+|lesson\\s*\\d+)[：:、.\\-\\s]*", "").trim();
        if (value.length() < 2 || value.length() > 120 || isNoise(value)) return;
        values.add(value);
    }

    private boolean isNoise(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return Set.of("首页", "登录", "注册", "更多", "课程", "目录", "返回顶部", "home", "login", "more").contains(lower);
    }

    private CourseOutlineVO result(String url, String title, String status, String message, List<String> items) {
        return CourseOutlineVO.builder().sourceUrl(url).sourceTitle(title).status(status)
                .message(message).items(items).fetchedAt(LocalDateTime.now()).build();
    }

    private record FetchResult(String url, Document document) {}
}
