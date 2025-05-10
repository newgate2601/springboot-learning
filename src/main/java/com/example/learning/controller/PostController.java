package com.example.learning.controller;

import com.example.learning.entity.PostEntity;
import com.example.learning.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/post")
@AllArgsConstructor
@CrossOrigin
public class PostController {
    private final PostRepository postRepository;

    @PostMapping
    public void save() {
        postRepository.save(
                PostEntity.builder()
                        .name(UUID.randomUUID().toString())
                        .code("code")
                        .postMappings(
                                List.of(
                                        com.example.learning.entity.PostMapping.builder()
                                                .a("a1")
                                                .b(1)
                                                .build(),
                                        com.example.learning.entity.PostMapping.builder()
                                                .a("a2")
                                                .b(2)
                                                .build()
                                )
                        )
                        .build()
        );
    }

    @GetMapping
    public PostEntity getById(String id) {
        return postRepository.findById(id).orElse(null);
    }

    @GetMapping("/list")
    public List<PostEntity> getList() {
        return postRepository.findAll();
    }
}
