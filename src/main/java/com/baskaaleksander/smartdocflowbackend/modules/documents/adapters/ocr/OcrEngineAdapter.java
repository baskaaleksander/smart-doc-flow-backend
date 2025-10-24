package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.ocr;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Image;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.OcrEnginePort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OcrEngineAdapter implements OcrEnginePort {

    private final OpenAiChatModel chatModel;
    @Value(value = "${spring.ai.openai.chat.options.model}")
    private String model;

    @Override
    public String extractText(List<Image> images) {

        var system = new SystemMessage("""
                You are an OCR engine. Extract plain text from each provided image.
                - Preserve original line breaks and spacing as much as possible.
                - Do not hallucinate missing text. If unreadable, return an empty string.
                - Use UTF-8 with diacritics intact.
                - Return ONLY valid JSON (no markdown). Schema:
                  {
                    "pages": [
                      { "page": <integer>, "text": "<string>" }
                    ]
                  }
                """);

        List<Media> medias = new ArrayList<>();
        for (Image img : images) {
            medias.add(toMedia(img.bytes(), img.pageNumber()));
        }

        UserMessage userMessage = UserMessage.builder()
                .text("Please OCR each page. Output must follow the schema above.")
                .media(medias)
                .build();

        var options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(1.0)
                .build();

        ChatResponse response = chatModel.call(
                new Prompt(
                        List.of(system, userMessage),
                        options
                )
        );


        return response.getResult().getOutput().getText();

    }

    private Media toMedia(byte[] bytes, int pageNumber) {
        return Media.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(bytes)
                .name("page-" + pageNumber + ".png")
                .build();
    }
}
