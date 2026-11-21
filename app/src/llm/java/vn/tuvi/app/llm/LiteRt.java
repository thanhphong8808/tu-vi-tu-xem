package vn.tuvi.app.llm;

import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Content;
import com.google.ai.edge.litertlm.Contents;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;
import com.google.ai.edge.litertlm.SamplerConfig;

import java.util.Collections;

/**
 * Lớp bọc mỏng quanh LiteRT-LM (D-57). Viết bằng Java để không phụ thuộc phiên bản metadata Kotlin của thư viện
 * (LiteRT-LM 0.19 biên dịch bằng Kotlin 2.4, dự án dùng kotlinc 2.0). Cùng mã này chạy trong app Android
 * (litertlm-android) và trên máy tính (litertlm-jvm, công cụ chấm 40 câu).
 *
 * Mỗi câu hỏi mở một Conversation mới (không nhớ câu trước): câu trả lời chỉ dựa vào dữ kiện của chính câu đó.
 */
public final class LiteRt implements AutoCloseable {
    private final Engine engine;
    private final String backend;

    private LiteRt(Engine engine, String backend) {
        this.engine = engine;
        this.backend = backend;
    }

    /**
     * Nạp model. [gpu] = thử GPU; lỗi thì người gọi tự lùi về CPU.
     * [cacheDir] là thư mục riêng của app (LiteRT-LM lưu bản biên dịch sẵn cho lần nạp sau nhanh hơn).
     */
    public static LiteRt open(String modelPath, boolean gpu, String cacheDir, int maxTokens) {
        Backend b = gpu ? new Backend.GPU() : new Backend.CPU();
        EngineConfig cfg = new EngineConfig(modelPath, b, null, null, maxTokens, null, cacheDir);
        Engine e = new Engine(cfg);
        try {
            e.initialize();
        } catch (RuntimeException ex) {
            try { e.close(); } catch (Throwable ignored) { }
            throw ex;
        }
        return new LiteRt(e, gpu ? "GPU" : "CPU");
    }

    public String backend() {
        return backend;
    }

    /** Sinh một câu trả lời (chặn luồng). Trả về văn bản thô của model. */
    public synchronized String generate(String system, String user, int maxOutputTokens, int topK, double topP, double temperature) {
        ConversationConfig cc = new ConversationConfig(
            Contents.Companion.of(system), Collections.<Message>emptyList(), Collections.emptyList(),
            new SamplerConfig(topK, topP, temperature, 0), true, null, Collections.<String, Object>emptyMap(), null, false,
            maxOutputTokens);
        Conversation c = engine.createConversation(cc);
        try {
            Message m = c.sendMessage(user);
            StringBuilder sb = new StringBuilder();
            for (Content x : m.getContents().getContents()) {
                if (x instanceof Content.Text) sb.append(((Content.Text) x).getText());
            }
            return sb.toString();
        } finally {
            c.close();
        }
    }

    @Override
    public synchronized void close() {
        engine.close();
    }
}
