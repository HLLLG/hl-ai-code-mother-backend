package com.hl.hlaicodemother.service;

import com.hl.hlaicodemother.model.dto.app.AppAddRequest;
import com.hl.hlaicodemother.model.dto.app.AppQueryRequest;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
public interface AppService extends IService<App> {

    /**
     * 与AI模型对话，生成代码
     * @param appId
     * @param message
     * @param user
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User user);

    /**
     * 停止 AI 生成代码
     *
     * @param appId 应用 id
     * @param user 当前用户
     * @return 是否存在并已发送停止信号
     */
    boolean stopChatToGenCode(Long appId, User user);

    /**
     * 创建应用
     *
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    Long addApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 部署应用
     * @param appId
     * @param user
     * @return
     */
    String deployApp(Long appId, User user);

    /**
     * 校验应用参数
     *
     * @param app 应用
     * @param add 是否为创建校验
     */
    void validApp(App app, boolean add);

    /**
     * 获取appvo对象
     *
     * @return 应用详情
     */
    AppVO getAppVO(App app);

    /**
     * 获取appvo列表
     *
     * @return 应用详情列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 校验应用归属
     *
     * @param app 应用
     * @param user 用户
     */
    void checkAppOwner(App app, User user);

    /**
     * 校验应用查看权限
     *
     * @param app 应用
     * @param user 用户
     */
    void checkAppViewAuth(App app, User user);

    /**
     * 构造用户查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

}
