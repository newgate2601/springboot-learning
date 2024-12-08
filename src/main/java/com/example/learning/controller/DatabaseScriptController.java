package com.example.learning.controller;

import com.example.learning.dto.DatabaseScriptRequest;
import com.example.learning.service.DatabaseScriptService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/script")
@AllArgsConstructor
@CrossOrigin
public class DatabaseScriptController {
    private final DatabaseScriptService databaseScriptService;

    @PostMapping
    public void scriptExecutive(@RequestBody DatabaseScriptRequest databaseScriptRequest){
        databaseScriptService.executive(databaseScriptRequest);
    }
}
