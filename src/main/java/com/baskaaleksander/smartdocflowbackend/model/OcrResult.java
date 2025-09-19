package com.baskaaleksander.smartdocflowbackend.model;

import java.util.List;

public record OcrResult(List<OcrResultPage> pages) {
}