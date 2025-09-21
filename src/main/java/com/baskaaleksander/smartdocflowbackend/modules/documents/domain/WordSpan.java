package com.baskaaleksander.smartdocflowbackend.modules.documents.domain;

public class WordSpan {
    final int start, end;

    public WordSpan(int s, int e) {
        start = s;
        end = e;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }
}
