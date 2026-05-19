package com.javeme.duobao.controller;

import com.javeme.duobao.common.BaseContext;
import com.javeme.duobao.dto.ReviewDTO;
import com.javeme.duobao.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/submit")
    public ResponseEntity<String> submit(@RequestBody ReviewDTO reviewDTO) {
        Long userId = BaseContext.getCurrentID();
        reviewService.submitReview(userId, reviewDTO);
        return ResponseEntity.ok("Review successfully submitted!");
    }
}
