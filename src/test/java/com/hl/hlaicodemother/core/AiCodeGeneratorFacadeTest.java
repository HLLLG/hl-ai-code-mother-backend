package com.hl.hlaicodemother.core;

import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("生成一个登录页面，代码不超过50行", CodeGenTypeEnum.MULTI_FILE, new App());
        Assertions.assertNotNull(file);
    }

    @Test
    void generateAndSaveCodeStream() {
    }
}
