package com.khi.ragservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rag-service")
@RequiredArgsConstructor
@Profile("local")
public class TestController {

  private final com.khi.ragservice.service.RagService ragService;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  @GetMapping("/test")
  public String test() {
    log.info("Test endpoint called - Retrieval Only");

    String user1Id = "test_user1";
    String user2Id = "test_user2";

    java.util.List<com.khi.ragservice.dto.ChatMessageDto> chatMessages = new java.util.ArrayList<>();

    com.khi.ragservice.dto.ChatMessageDto msg1 = new com.khi.ragservice.dto.ChatMessageDto();
    msg1.setUserId(user1Id);
    msg1.setName("철수");
    msg1.setMessage("너는 왜 맨날 그 모양이니? 진짜 짜증나게."); // RAG should find conflict/mistake related items
    chatMessages.add(msg1);

    com.khi.ragservice.dto.ChatMessageDto msg2 = new com.khi.ragservice.dto.ChatMessageDto();
    msg2.setUserId(user2Id);
    msg2.setName("영희");
    msg2.setMessage("뭐라고? 말을 왜 그렇게 심하게 해?");
    chatMessages.add(msg2);

    try {
      java.util.Map<String, Object> result = ragService.prepareRAGContext(user1Id, user2Id, chatMessages);
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    } catch (Exception e) {
      log.error("Error during retrieval test", e);
      return "Error: " + e.getMessage();
    }
  }
}
