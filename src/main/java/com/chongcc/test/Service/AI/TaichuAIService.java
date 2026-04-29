package com.chongcc.test.Service.AI;

import reactor.core.publisher.Flux;

public interface TaichuAIService {
    Flux<String> streamChat(String message);
}
