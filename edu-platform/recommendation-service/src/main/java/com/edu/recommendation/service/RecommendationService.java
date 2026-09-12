package com.edu.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.recommendation.dto.*;
import com.edu.recommendation.entity.*;
import com.edu.recommendation.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private static final int VECTOR_SIZE = 384;
    private static final Map<String, Double> BEHAVIOR_WEIGHTS = Map.ofEntries(
            Map.entry("impression", 0.1), Map.entry("click", 1.0), Map.entry("favorite", 3.0),
            Map.entry("order", 4.0), Map.entry("purchase", 6.0), Map.entry("start_learning", 7.0),
            Map.entry("progress", 5.0), Map.entry("complete", 10.0), Map.entry("rating", 8.0),
            Map.entry("search", 1.5));

    private final CourseSnapshotMapper courseMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final RecommendationLogMapper logMapper;

    @Value("${recommendation.interest-weight:0.36}") private double interestWeight;
    @Value("${recommendation.goal-weight:0.34}") private double goalWeight;
    @Value("${recommendation.profile-weight:0.12}") private double profileWeight;
    @Value("${recommendation.popularity-weight:0.10}") private double popularityWeight;
    @Value("${recommendation.quality-weight:0.08}") private double qualityWeight;
    @Value("${recommendation.level-bonus:0.03}") private double levelBonus;

    @Transactional
    public RecommendationResponse recommend(Long userId, RecommendationRequest request) {
        String requestId = UUID.randomUUID().toString();
        List<CourseSnapshot> courses = courseMapper.selectList(new LambdaQueryWrapper<CourseSnapshot>()
                .eq(CourseSnapshot::getStatus, 1)
                .orderByDesc(CourseSnapshot::getStudentCount));

        List<UserBehavior> behaviors = loadBehaviors(userId, request.getSessionId());
        Set<Long> purchased = behaviors.stream()
                .filter(b -> Set.of("purchase", "complete").contains(b.getBehaviorType()))
                .map(UserBehavior::getCourseId).collect(Collectors.toSet());
        Map<Long, CourseSnapshot> byId = courses.stream().collect(Collectors.toMap(CourseSnapshot::getId, c -> c));
        String profileText = buildProfileText(behaviors, byId);
        double[] interestVector = vectorize(request.getInterest());
        double[] goalVector = vectorize(request.getGoal());
        double[] profileVector = vectorize(profileText);
        int maxStudents = courses.stream().map(CourseSnapshot::getStudentCount).filter(Objects::nonNull).max(Integer::compareTo).orElse(1);

        List<Scored> scored = courses.stream().filter(c -> !purchased.contains(c.getId())).map(c -> {
            double[] courseVector = vectorize(courseText(c));
            double interest = hasText(request.getInterest()) ? cosine(interestVector, courseVector) : 0.5;
            double goal = hasText(request.getGoal()) ? cosine(goalVector, courseVector) : 0.5;
            double profile = hasText(profileText) ? cosine(profileVector, courseVector) : 0.5;
            double popularity = Math.log1p(nvl(c.getStudentCount())) / Math.log1p(Math.max(1, maxStudents));
            double quality = c.getRating() == null ? 0.5 : c.getRating().doubleValue() / 5.0;
            double levelFit = hasText(request.getLevel()) && request.getLevel().equals(c.getLevel()) ? 1.0 : 0.0;
            double total = interest * interestWeight + goal * goalWeight + profile * profileWeight
                    + popularity * popularityWeight + quality * qualityWeight + levelFit * levelBonus;
            return new Scored(c, clamp(interest), clamp(goal), clamp(profile), levelFit,
                    clamp(popularity), clamp(quality), clamp(total));
        }).sorted(Comparator.comparingDouble(Scored::total).reversed()).toList();

        List<Scored> selected = diversify(scored, request.getLimit() == null ? 6 : request.getLimit());
        List<RecommendedCourse> results = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            Scored s = selected.get(i);
            String reason = reason(s, request, hasText(profileText));
            results.add(toDto(s, reason));
            RecommendationLog log = new RecommendationLog();
            log.setRequestId(requestId); log.setUserId(userId); log.setSessionId(request.getSessionId());
            log.setCourseId(s.course().getId()); log.setRankPosition(i + 1);
            log.setScore(BigDecimal.valueOf(s.total()).setScale(4, RoundingMode.HALF_UP));
            log.setReason(reason); log.setClicked(0); log.setPurchased(0); log.setCreatedAt(LocalDateTime.now());
            logMapper.insert(log);
        }
        return RecommendationResponse.builder().requestId(requestId)
                .strategy("intent-first-interest-goal-v2")
                .personalized(userId != null && !behaviors.isEmpty()).courses(results).build();
    }

    @Transactional
    public void recordBehavior(Long userId, BehaviorRequest request) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId); behavior.setSessionId(request.getSessionId()); behavior.setCourseId(request.getCourseId());
        behavior.setBehaviorType(request.getBehaviorType()); behavior.setBehaviorValue(request.getBehaviorValue());
        behavior.setContextJson(request.getContextJson()); behavior.setCreatedAt(LocalDateTime.now());
        behaviorMapper.insert(behavior);
        if (hasText(request.getRequestId()) && Set.of("click", "purchase").contains(request.getBehaviorType())) {
            RecommendationLog update = new RecommendationLog();
            if ("click".equals(request.getBehaviorType())) update.setClicked(1); else update.setPurchased(1);
            logMapper.update(update, new LambdaQueryWrapper<RecommendationLog>()
                    .eq(RecommendationLog::getRequestId, request.getRequestId())
                    .eq(RecommendationLog::getCourseId, request.getCourseId()));
        }
    }

    private List<UserBehavior> loadBehaviors(Long userId, String sessionId) {
        if (userId == null && !hasText(sessionId)) return List.of();
        LambdaQueryWrapper<UserBehavior> q = new LambdaQueryWrapper<UserBehavior>().orderByDesc(UserBehavior::getCreatedAt).last("limit 500");
        if (userId != null && hasText(sessionId)) q.and(w -> w.eq(UserBehavior::getUserId, userId).or().eq(UserBehavior::getSessionId, sessionId));
        else if (userId != null) q.eq(UserBehavior::getUserId, userId);
        else q.eq(UserBehavior::getSessionId, sessionId);
        return behaviorMapper.selectList(q);
    }

    private String buildProfileText(List<UserBehavior> behaviors, Map<Long, CourseSnapshot> courses) {
        StringBuilder text = new StringBuilder();
        behaviors.forEach(b -> {
            CourseSnapshot c = courses.get(b.getCourseId());
            if (c == null) return;
            int repeats = Math.max(1, (int)Math.round(BEHAVIOR_WEIGHTS.getOrDefault(b.getBehaviorType(), 1.0)));
            if ("rating".equals(b.getBehaviorType()) && b.getBehaviorValue() != null && b.getBehaviorValue().doubleValue() < 3) repeats = -2;
            if (repeats > 0) for (int i = 0; i < Math.min(repeats, 10); i++) text.append(' ').append(courseText(c));
        });
        return text.toString();
    }

    private List<Scored> diversify(List<Scored> ranked, int requested) {
        int limit = Math.max(1, Math.min(requested, 20));
        List<Scored> pool = new ArrayList<>(ranked.subList(0, Math.min(ranked.size(), Math.max(limit * 4, 20))));
        List<Scored> result = new ArrayList<>();
        Map<Long, Integer> categories = new HashMap<>();
        while (!pool.isEmpty() && result.size() < limit) {
            Scored best = pool.stream().max(Comparator.comparingDouble(s ->
                    s.total() - categories.getOrDefault(s.course().getCategoryId(), 0) * 0.08)).orElseThrow();
            result.add(best); pool.remove(best);
            categories.merge(best.course().getCategoryId(), 1, Integer::sum);
        }
        return result;
    }

    private RecommendedCourse toDto(Scored s, String reason) {
        CourseSnapshot c = s.course();
        return RecommendedCourse.builder().id(c.getId()).title(c.getTitle()).description(c.getDescription())
                .coverImage(c.getCoverImage()).linkUrl(c.getLinkUrl()).teacherId(c.getTeacherId()).teacherName(c.getTeacherName())
                .categoryId(c.getCategoryId()).price(c.getPrice()).duration(c.getDuration()).level(c.getLevel()).status(c.getStatus())
                .studentCount(c.getStudentCount()).rating(c.getRating()).tags(c.getTags()).recommendReason(reason)
                .matchScore(percent(s.total())).scoreDetails(Map.of("interest", percent(s.interest()), "goal", percent(s.goal()),
                        "profile", percent(s.profile()), "level", percent(s.levelFit()),
                        "popularity", percent(s.popularity()), "quality", percent(s.quality()))).build();
    }

    private String reason(Scored s, RecommendationRequest r, boolean personalized) {
        List<String> parts = new ArrayList<>();
        if (s.interest() >= .55 && hasText(r.getInterest())) parts.add("贴合学习兴趣“" + r.getInterest() + "”");
        if (s.goal() >= .55 && hasText(r.getGoal())) parts.add("有助于实现“" + r.getGoal() + "”");
        if (personalized && s.profile() >= .55) parts.add("符合你的历史学习偏好");
        if (parts.size() < 2 && s.levelFit() > 0) parts.add("难度适合当前水平");
        if (s.quality() >= .9) parts.add("课程评分较高");
        if (parts.isEmpty()) parts.add("综合课程质量与热度推荐");
        return String.join("，", parts);
    }

    private double[] vectorize(String text) {
        double[] v = new double[VECTOR_SIZE];
        if (!hasText(text)) return v;
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        List<String> features = new ArrayList<>(Arrays.asList(normalized.split("\\s+")));
        String compact = normalized.replace(" ", "");
        for (int i = 0; i < compact.length() - 1; i++) features.add(compact.substring(i, i + 2));
        for (String f : features) {
            if (f.isBlank()) continue;
            int hash = Arrays.hashCode(f.getBytes(StandardCharsets.UTF_8));
            int index = Math.floorMod(hash, VECTOR_SIZE);
            v[index] += (hash & 1) == 0 ? 1 : -1;
        }
        double norm = Math.sqrt(Arrays.stream(v).map(x -> x * x).sum());
        if (norm > 0) for (int i = 0; i < v.length; i++) v[i] /= norm;
        return v;
    }

    private double cosine(double[] a, double[] b) {
        double dot = 0; for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return clamp((dot + 1) / 2);
    }
    private String courseText(CourseSnapshot c) { return join(c.getTitle(), c.getDescription(), c.getTags(), c.getLevel()); }
    private String join(String... values) { return Arrays.stream(values).filter(this::hasText).collect(Collectors.joining(" ")); }
    private boolean hasText(String v) { return v != null && !v.isBlank(); }
    private int nvl(Integer v) { return v == null ? 0 : v; }
    private double clamp(double v) { return Math.max(0, Math.min(1, v)); }
    private int percent(double v) { return (int)Math.round(clamp(v) * 100); }
    private record Scored(CourseSnapshot course, double interest, double goal, double profile, double levelFit,
                          double popularity, double quality, double total) {}
}
