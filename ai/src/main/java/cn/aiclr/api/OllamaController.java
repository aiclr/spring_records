package cn.aiclr.api;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class OllamaController {

    @Resource
    private OllamaChatModel chatModel;

    @GetMapping("ai")
    public String ai(@RequestParam(value = "msg", defaultValue = "你是谁") String msg) {
        return chatModel.call(msg);
    }

    @GetMapping("aiStream")
    public Flux<String> aiStream(@RequestParam(value = "msg", defaultValue = "你是谁") String msg) {
        return chatModel.stream(msg);
    }


    @GetMapping(value = "aiSystem")
    public Flux<String> aiSystem(@RequestParam(value = "msg", defaultValue = "你是谁") String msg) {
        Message system = SystemMessage.builder().text("你是一个计算机系老师，你只回答编程相关问题").build();
        Message user = UserMessage.builder().text(msg).build();
        return chatModel.stream(system, user);
    }
}
