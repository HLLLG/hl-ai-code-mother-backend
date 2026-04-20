package com.hl.hlaicodemother.service;

import com.hl.hlaicodemother.model.dto.app.AppAddRequest;
import com.hl.hlaicodemother.model.dto.app.AppQueryRequest;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.vo.AppVO;
import com.mybatisflex.core.paginate.Page;
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
     *
     * @param appId
     * @param message
     * @param isAdd
     * @param user
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, Boolean isAdd, User user);

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
     * 异步生成应用截图
     *
     * @param appId 应用 id
     * @param appDeployUrl 应用部署地址
     */
    void generateAppScreenshotAsync(Long appId, String appDeployUrl);

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
     * 获取可缓存的应用详情（仅包含稳定字段，不含用户态实时信息）。
     *
     * @param id 应用 id
     * @return 应用详情
     */
    AppVO getAppVOByIdCacheable(Long id);

    /**
     * 失效应用详情缓存。
     *
     * @param appId 应用 id
     */
    void invalidateAppDetailCache(Long appId);

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
     * 校验应用编辑权限
     *
     * @param app 应用
     * @param user 用户
     */
    void checkAppEditAuth(App app, User user);

    /**
     * 构造用户查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 应用代码下载次数 +1（原子更新）
     *
     * @param app 应用 id
     */
    void incrementDownloadCount(App app);

    /**
     * 分页查询"精选应用"列表（带旁路缓存 + 空值哨兵 + 防击穿）。
     * <p>
     * 仅在 pageNum &lt;= 10 的热点页使用缓存，冷门深翻页直接查库，避免污染缓存。
     *
     * @param appQueryRequest 查询请求
     * @return 分页结果（AppVO）
     */
    Page<AppVO> listGoodAppVOByPage(AppQueryRequest appQueryRequest);

    /**
     * 失效"精选应用分页"全量缓存（按前缀 SCAN 删除 + 延时双删）。
     * <p>
     * 调用方：所有可能影响精选列表内容的写操作（删除应用、修改 priority 等），
     * 应在<b>事务提交后</b>调用。
     */
    void invalidateGoodAppPageCache();

}
