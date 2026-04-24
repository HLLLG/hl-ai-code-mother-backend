package com.hl.hlaicodemother.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 监控上下文
 *
 * @author hl
 * @date 2023/09/05
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonitorContext implements Serializable {

    @Serial
    private static final long serialVersionUID = -3623576004250747472L;

    private String userId;

    private String appId;


}
