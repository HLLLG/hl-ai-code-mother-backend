package com.hl.hlaicodemother.core;

import com.hl.hlaicodemother.ai.AiCodeGeneratorService;
import com.hl.hlaicodemother.ai.AiGenerationTaskManager;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.service.AppVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCodeGeneratorFacadeTest {

    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    private AiGenerationTaskManager aiGenerationTaskManager;

    @Mock
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Mock
    private AppVersionService appVersionService;

    @BeforeEach
    void setUp() {
        aiCodeGeneratorFacade = new AiCodeGeneratorFacade();
        aiGenerationTaskManager = new AiGenerationTaskManager();
        ReflectionTestUtils.setField(aiCodeGeneratorFacade, "aiCodeGeneratorService", aiCodeGeneratorService);
        ReflectionTestUtils.setField(aiCodeGeneratorFacade, "appVersionService", appVersionService);
        ReflectionTestUtils.setField(aiCodeGeneratorFacade, "aiGenerationTaskManager", aiGenerationTaskManager);
    }

    @Test
    void generateAndSaveCodeStreamShouldSkipSaveWhenStopped() throws InterruptedException {
        App app = new App();
        app.setId(1L);
        app.setCurrentVersion(1);
        String taskKey = "1_1";
        when(aiCodeGeneratorService.generateHtmlCodeStream("test"))
                .thenReturn(Flux.concat(Flux.just("partial code"), Flux.never()));

        List<String> resultList = new CopyOnWriteArrayList<>();
        CountDownLatch completeLatch = new CountDownLatch(1);

        aiCodeGeneratorFacade.generateAndSaveCodeStream("test", CodeGenTypeEnum.HTML, app, taskKey)
                .doOnNext(chunk -> {
                    resultList.add(chunk);
                    aiGenerationTaskManager.cancelTask(taskKey);
                })
                .doOnComplete(completeLatch::countDown)
                .subscribe();

        assertTrue(completeLatch.await(1, TimeUnit.SECONDS));
        assertEquals(List.of("partial code"), resultList);
        verify(appVersionService, never()).addVersion(any(App.class), anyString());
    }
}
