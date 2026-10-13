package com.xiaodu.user.service;

import com.xiaodu.common.domain.dto.PageDTO;
import com.xiaodu.user.domain.query.UserPageQuery;
import com.xiaodu.user.domain.vo.StaffVO;

/**
 * <p>
 * 员工详情表 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2022-07-12
 */
public interface IStaffService {
    PageDTO<StaffVO> queryStaffPage(UserPageQuery pageQuery);
}
