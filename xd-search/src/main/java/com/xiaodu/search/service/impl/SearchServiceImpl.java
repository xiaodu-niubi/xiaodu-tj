package com.xiaodu.search.service.impl;

import com.xiaodu.api.cache.CategoryCache;
import com.xiaodu.api.client.course.CourseClient;
import com.xiaodu.api.client.user.UserClient;
import com.xiaodu.api.dto.course.CourseSearchDTO;
import com.xiaodu.api.dto.user.UserDTO;
import com.xiaodu.common.constants.ErrorInfo;
import com.xiaodu.common.domain.dto.PageDTO;
import com.xiaodu.common.exceptions.CommonException;
import com.xiaodu.common.utils.*;
import com.xiaodu.search.config.InterestsProperties;
import com.xiaodu.search.constants.SearchErrorInfo;
import com.xiaodu.search.domain.po.Course;
import com.xiaodu.search.domain.query.CoursePageQuery;
import com.xiaodu.search.domain.vo.CourseVO;
import com.xiaodu.search.repository.CourseRepository;
import com.xiaodu.search.service.IInterestsService;
import com.xiaodu.search.service.ISearchService;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.xiaodu.search.repository.CourseRepository.PUBLISH_TIME;

@Service
public class SearchServiceImpl implements ISearchService {

    @Autowired
    private RestHighLevelClient restClient;

    @Autowired
    private IInterestsService interestsService;

    @Autowired
    private UserClient userClient;

    @Autowired
    private CategoryCache categoryCache;

    @Autowired
    private InterestsProperties interestsProperties;

    @Autowired
    private CourseClient courseClient;

    @Autowired
    private CourseRepository courseRepository;

    @Override
    public List<CourseVO> queryCourseByCateId(Long cateLv2Id) {
        return queryTopNByCategoryIdLv2sAndFree(
                CollUtils.singletonList(cateLv2Id), null, PUBLISH_TIME, false, 10);
    }

    @Override
    public List<CourseVO> queryBestTopN() {
        // 1.获取当前用户
        return queryTopNCourseOnMarketByFree(false, CourseRepository.SOLD);
    }

    @Override
    public List<CourseVO> queryNewTopN() {
        return queryTopNCourseOnMarketByFree(false, PUBLISH_TIME);
    }

    @Override
    public List<CourseVO> queryFreeTopN() {
        return queryTopNCourseOnMarketByFree(true, CourseRepository.SOLD);
    }

    private List<CourseVO> queryTopNCourseOnMarketByFree(boolean isFree, String sortBy) {
        // 1.获取当前用户
        Long id = UserContext.getUser();
        // 2.查询课程
        List<CourseVO> courses = null;
        if (id == null) {
            // 3.未登录，直接查询报名人数最多的
            courses = queryTopNByCategoryIdLv2sAndFree(
                    null, isFree, sortBy, false, interestsProperties.getTopNumber());
        } else {
            // 4.已登录，根据兴趣爱好查询
            List<Long> categoryIds = interestsService.queryMyInterestsIds();
            if (CollUtils.isEmpty(categoryIds)) {
                // 4.1.没有兴趣爱好，直接查询报名人数最多的
                courses = queryTopNByCategoryIdLv2sAndFree(
                        null, isFree, sortBy, false, interestsProperties.getTopNumber());
            } else {
                // 4.2.有爱好.查询爱好课程中报名人数最多的
                courses = queryTopNByCategoryIdLv2sAndFree(
                        categoryIds, isFree, sortBy, false, interestsProperties.getTopNumber());
            }
        }
        return courses;
    }

    private List<CourseVO> queryTopNByCategoryIdLv2sAndFree(
            List<Long> categoryIds, Boolean isFree, String sortBy, boolean isASC, int n) {
        // 1.准备Request
        SearchRequest request = new SearchRequest(CourseRepository.INDEX_NAME);
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 1.1.是否免费
        if(isFree != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.FREE, isFree));
        }
        // 1.2.分类id
        if (categoryIds != null) {
            if (categoryIds.size() == 1) {
                queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.CATEGORY_ID_LV2, categoryIds.get(0)));
            } else {
                queryBuilder.filter(QueryBuilders.termsQuery(CourseRepository.CATEGORY_ID_LV2, categoryIds));
            }
        }
        if(isFree != null || categoryIds != null) {
            request.source().query(queryBuilder);
        }
        // 1.3.TopN
        request.source().size(n).sort(sortBy, isASC ? SortOrder.ASC : SortOrder.DESC);
        // 2.发送请求
        SearchResponse response = null;
        try {
            response = restClient.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new CommonException(SearchErrorInfo.QUERY_COURSE_ERROR, e);
        }
        // 3.解析
        SearchHits searchHits = response.getHits();
        SearchHit[] hits = searchHits.getHits();
        if (hits == null || hits.length == 0) {
            return fallbackQueryTopN(categoryIds, isFree, sortBy, isASC, n);
        }
        return parseHitsToCourseVOList(hits);
    }

    private List<CourseVO> fallbackQueryTopN(
            List<Long> categoryIds, Boolean isFree, String sortBy, boolean isASC, int n) {
        // ES无数据，降级查数据库
        List<CourseSearchDTO> dbResults = courseClient.queryCoursesForSearch(
                null, null, null, null, categoryIds, isFree, null, sortBy, isASC, 0, n);
        if (CollUtils.isEmpty(dbResults)) {
            return CollUtils.emptyList();
        }
        // 保存到ES
        saveCoursesToEs(dbResults);
        // 转换为VO并返回
        return convertToCourseVOList(dbResults);
    }

    private List<CourseVO> parseHitsToCourseVOList(SearchHit[] hits) {
        List<CourseVO> courses = new ArrayList<>(hits.length);
        Set<Long> teacherIds = new HashSet<>(hits.length);
        for (SearchHit hit : hits) {
            CourseVO vo = JsonUtils.toBean(hit.getSourceAsString(), CourseVO.class);
            teacherIds.add(Long.valueOf(vo.getTeacher()));
            courses.add(vo);
        }
        teacherIds.remove(0L);
        if (teacherIds.size() == 0) {
            return courses;
        }
        // 4.查询教师
        List<UserDTO> teachers = userClient.queryUserByIds(teacherIds);
        AssertUtils.isNotEmpty(teachers, SearchErrorInfo.TEACHER_NOT_EXISTS);
        Map<String, String> tMap = teachers.stream()
                .collect(Collectors.toMap(t -> t.getId().toString(), UserDTO::getName));
        for (CourseVO c : courses) {
            c.setTeacher(tMap.getOrDefault(c.getTeacher(), "匿名"));
        }
        return courses;
    }

    private void saveCoursesToEs(List<CourseSearchDTO> dtos) {
        List<Course> courses = new ArrayList<>(dtos.size());
        for (CourseSearchDTO dto : dtos) {
            Course course = BeanUtils.toBean(dto, Course.class);
            course.setType(dto.getCourseType());
            courses.add(course);
        }
        courseRepository.saveAll(courses);
    }

    private List<CourseVO> convertToCourseVOList(List<CourseSearchDTO> dtos) {
        List<CourseVO> vos = new ArrayList<>(dtos.size());
        Set<Long> teacherIds = new HashSet<>(dtos.size());
        for (CourseSearchDTO dto : dtos) {
            CourseVO vo = BeanUtils.toBean(dto, CourseVO.class);
            vo.setTeacher(dto.getTeacher() != null ? dto.getTeacher().toString() : "0");
            teacherIds.add(dto.getTeacher());
            vos.add(vo);
        }
        teacherIds.remove(0L);
        if (!teacherIds.isEmpty()) {
            List<UserDTO> teachers = userClient.queryUserByIds(teacherIds);
            if (CollUtils.isNotEmpty(teachers)) {
                Map<Long, String> tMap = teachers.stream()
                        .collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
                for (CourseVO vo : vos) {
                    Long tId = Long.valueOf(vo.getTeacher());
                    vo.setTeacher(tMap.getOrDefault(tId, "匿名"));
                }
            }
        }
        return vos;
    }

    @Override
    public PageDTO<CourseVO> queryCoursesForPortal(CoursePageQuery query) {
        // 1.搜索数据
        SearchResponse response = searchForResponse(query, CourseVO.EXCLUDE_FIELDS);
        // 2.解析响应
        PageDTO<Course> result = handleSearchResponse(response, query.getPageSize());
        // 3.处理VO
        List<Course> list = result.getList();
        if (CollUtils.isEmpty(list)) {
            // ES无数据，降级查数据库
            return fallbackQueryCoursesForPortal(query);
        }
        // 3.1.查询教师信息
        List<Long> teacherIds = list.stream().map(Course::getTeacher).collect(Collectors.toList());
        List<UserDTO> teachers = userClient.queryUserByIds(teacherIds);
        AssertUtils.isNotEmpty(teachers, SearchErrorInfo.TEACHER_NOT_EXISTS);
        Map<Long, String> teacherMap = teachers.stream()
                .collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        // 3.2.转换VO
        List<CourseVO> vos = new ArrayList<>(list.size());
        for (Course c : list) {
            CourseVO vo = BeanUtils.toBean(c, CourseVO.class);
            vo.setTeacher(teacherMap.getOrDefault(c.getTeacher(), "未知"));
            vos.add(vo);
        }
        return new PageDTO<>(result.getTotal(), result.getPages(), vos);
    }

    private PageDTO<CourseVO> fallbackQueryCoursesForPortal(CoursePageQuery query) {
        List<CourseSearchDTO> dbResults = courseClient.queryCoursesForSearch(
                query.getKeyword(), query.getCategoryIdLv1(), query.getCategoryIdLv2(),
                query.getCategoryIdLv3(), null, query.getFree(), query.getType(),
                query.getSortBy(), query.getIsAsc(), query.from(), query.getPageSize());
        if (CollUtils.isEmpty(dbResults)) {
            return PageDTO.empty(0L, 0L);
        }
        // 保存到ES
        saveCoursesToEs(dbResults);
        // 转换为VO
        List<CourseVO> vos = convertToCourseVOList(dbResults);
        return new PageDTO<>((long) vos.size(), 1L, vos);
    }

    @Override
    public List<Long> queryCoursesIdByName(String keyword) {
        // 1.创建Request
        SearchRequest request = new SearchRequest(CourseRepository.INDEX_NAME);
        // 2.构建DSL
        request.source()
                .query(QueryBuilders.matchPhraseQuery(CourseRepository.DEFAULT_QUERY_NAME, keyword))
                .fetchSource(new String[]{"id"}, null);
        // 3.查询ES
        SearchResponse response;
        try {
            response = restClient.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new CommonException(SearchErrorInfo.QUERY_COURSE_ERROR, e);
        }
        // 4.解析
        SearchHits searchHits = response.getHits();
        SearchHit[] hits = searchHits.getHits();
        if (hits.length > 0) {
            return Arrays.stream(hits)
                    .map(SearchHit::getId)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }
        // 5.ES无数据，降级查数据库
        return courseClient.queryCoursesIdByName(keyword);
    }


    private SearchResponse searchForResponse(CoursePageQuery query, String[] excludeFields) {
        // 1.创建Request
        SearchRequest request = new SearchRequest(CourseRepository.INDEX_NAME);
        // 2.构建DSL
        // 2.1.构建query
        buildBasicQuery(request, query);
        // 2.2.排序
        String sortBy = query.getSortBy();
        if (StringUtils.isNotBlank(sortBy)) {
            request.source().sort(sortBy, query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);
        }
        // 2.3.分页
        request.source().from(query.from()).size(query.getPageSize());
        // 2.4.高亮
        request.source().highlighter(new HighlightBuilder().field(CourseRepository.DEFAULT_QUERY_NAME));
        // 2.5.source处理
        request.source().fetchSource(null, excludeFields);
        // 3.发送请求
        SearchResponse response = null;
        try {
            response = restClient.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new CommonException(ErrorInfo.Msg.SERVER_INTER_ERROR, e);
        }
        return response;
    }

    private void buildBasicQuery(SearchRequest request, CoursePageQuery query) {
        // 1.准备bool查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 2.关键字搜索
        String keyword = query.getKeyword();
        if (StringUtils.isBlank(keyword)) {
            queryBuilder.must(QueryBuilders.matchAllQuery());
        } else {
            queryBuilder.must(QueryBuilders.matchPhraseQuery(CourseRepository.DEFAULT_QUERY_NAME, keyword));
        }
        // 3.其它条件
        if (query.getCategoryIdLv1() != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.CATEGORY_ID_LV1, query.getCategoryIdLv1()));
        }
        if (query.getCategoryIdLv2() != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.CATEGORY_ID_LV2, query.getCategoryIdLv2()));
        }
        if (query.getCategoryIdLv3() != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.CATEGORY_ID_LV3, query.getCategoryIdLv3()));
        }
        if (query.getFree() != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.FREE, query.getFree()));
        }
        if (query.getType() != null) {
            queryBuilder.filter(QueryBuilders.termQuery(CourseRepository.TYPE, query.getType()));
        }
        LocalDateTime beginTime = query.getBeginTime();
        LocalDateTime endTime = query.getEndTime();
        if(beginTime != null || endTime != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(CourseRepository.UPDATE_TIME);
            if (beginTime != null) {
                rangeQuery.gte(beginTime);
            }
            if (endTime != null) {
                rangeQuery.lte(endTime);
            }
            queryBuilder.filter(rangeQuery);
        }
        // 4.写入request
        request.source().query(queryBuilder);
    }

    private PageDTO<Course> handleSearchResponse(SearchResponse response, int pageSize) {
        SearchHits searchHits = response.getHits();
        // 1.总条数
        long total = searchHits.getTotalHits().value;
        // 2.总页数
        long totalPages = (total + pageSize - 1) / pageSize;
        // 3.获取命中的数据
        SearchHit[] hits = searchHits.getHits();
        if (hits.length <= 0) {
            return new PageDTO<>(total, totalPages, CollUtils.emptyList());
        }
        // 4.遍历
        List<Course> list = new ArrayList<>(hits.length);
        for (SearchHit hit : hits) {
            // 5.获取某一条source
            String jsonSource = hit.getSourceAsString();
            // 6.反序列化
            Course course = JsonUtils.toBean(jsonSource, Course.class);
            // 7.处理高亮
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (CollUtils.isNotEmpty(highlightFields)) {
                // 7.1.获取高亮结果
                HighlightField field = highlightFields.get(CourseRepository.DEFAULT_QUERY_NAME);
                Object[] fragments = field.getFragments();
                String value = StringUtils.join(fragments);
                // 7.2.覆盖非高亮结果
                course.setName(value);
            }
            list.add(course);
        }
        return new PageDTO<>(total, totalPages, list);
    }
}
