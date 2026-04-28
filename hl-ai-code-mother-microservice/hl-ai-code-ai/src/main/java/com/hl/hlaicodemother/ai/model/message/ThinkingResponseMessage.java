package com.hl.hlaicodemother.ai.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ThinkingResponseMessage extends StreamMessage{

    private String text;

    public ThinkingResponseMessage(String text) {
        super(StreamMessageTypeEnum.THINKING_RESPONSE.getValue());
        this.text = text;
    }

}
