package com.hl.hlaicodemother.generater;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.ColumnConfig;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

public class MyBatisCodeGenerator {

    private static final String[] TABLE_NAME = {"app_version"};

    public static void main(String[] args) {
        // 获取数据库连接信息
        Dict dict = YamlUtil.loadByPath("application.yml");
        Map<String, Object> datasourceConfig = dict.getByPath(
                "spring.datasource");
        String url = String.valueOf(datasourceConfig.get("url"));
        String username = String.valueOf(datasourceConfig.get("username"));
        String password = String.valueOf(datasourceConfig.get("password"));

        //配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        //创建配置内容
        GlobalConfig globalConfig = createGlobalConfig();

        //通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        //生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfig() {
        //创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        //设置根包
        globalConfig.getPackageConfig().setBasePackage("com.hl.hlaicodemother.genresult");

        //设置表前缀和只生成哪些表，setGenerateTable 未配置时，生成所有表
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAME)
                // 设置逻辑删除字段名称
                .setLogicDeleteColumn("isDelete");

        //设置生成 entity 并启用 Lombok
        globalConfig.enableEntity().setWithLombok(true).setJdkVersion(21);

        //设置生成 mapper
        globalConfig.enableMapper();
        globalConfig.enableMapperXml();

        // 设置生成 service
        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        // 设置生成 controller
        globalConfig.enableController();

        // 设置生成时间和字符串为空， 避免多余的代码改动
        globalConfig.getJavadocConfig()
                .setAuthor("<a href=\"https://github.com/HLLLG\">程序员HL</a>")
                .setSince("");
        return globalConfig;
    }
}