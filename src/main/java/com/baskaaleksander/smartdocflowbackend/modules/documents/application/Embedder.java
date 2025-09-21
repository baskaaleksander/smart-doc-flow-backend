package com.baskaaleksander.smartdocflowbackend.modules.documents.application;

import java.util.List;

public interface Embedder {

    List<float[]> embedBatch(List<String> texts);
}
