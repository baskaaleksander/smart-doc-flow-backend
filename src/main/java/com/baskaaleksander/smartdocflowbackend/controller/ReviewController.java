package com.baskaaleksander.smartdocflowbackend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
public class ReviewController {
}
