package com.baskaaleksander.smartdocflowbackend.modules.documents.application;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.Chunk;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.SentenceSpan;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChunkerService {

    private final Tokenizer tokenizer;

    public ChunkerService(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }


    public List<Chunk> chunkPage(String rawText, UUID documentId, int page) {

        return null;
    }

    private List<SentenceSpan> splitIntoSentenceSpans(String rawText) {
        Pattern p = Pattern.compile(".+?(?<=[.!?])(?=\\s+|$)", Pattern.DOTALL | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(rawText);

        List<SentenceSpan> spans = new ArrayList<>();
        int lastEnd = 0;

        while(m.find()) {
            int s = m.start();
            int e = m.end();
            String piece = rawText.substring(s, e).trim();

            if(!piece.isEmpty()) {
                int leading = leadingWs(rawText, s, e);
                int trailing = trailingWs(rawText, s, e);
                int startAdj = s + leading;
                int endAdj = e - trailing;

                spans.add(new SentenceSpan(startAdj, endAdj, rawText.substring(startAdj, endAdj)));
                lastEnd = e;
            }
        }

        if (spans.isEmpty() && !rawText.isBlank()) {
            spans.add(new SentenceSpan(0, rawText.length(), rawText));
        }

        return spans;
    }

    private int leadingWs(String t, int s, int e) {
        int i = s;
        while (i < e && Character.isWhitespace(t.charAt(i))) i++;
        return i - s;
    }

    private int trailingWs(String t, int s, int e) {
        int i = e - 1;
        while(i > e && Character.isWhitespace(t.charAt(i))) i--;

        return (e - 1) - i;
    }
}
