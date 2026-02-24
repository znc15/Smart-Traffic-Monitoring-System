import { useState, useRef, useEffect, memo, useCallback } from "react";
import {
  loadChatHistory,
  saveChatHistory,
  clearChatHistory,
  loadChatDraft,
  saveChatDraft,
  clearChatDraft,
} from "@/utils/chatStorage";
import { apiConfig, authConfig, endpoints } from "@/config";
// Helper: check if an URL points to the same API origin (handles localhost vs 127.0.0.1)
function isSameApiOrigin(url: string): boolean {
  try {
    const target = new URL(url, window.location.origin);
    const apiBase = new URL(apiConfig.API_HTTP_BASE);
    return target.origin === apiBase.origin;
  } catch {
    return false;
  }
}

// Component fetch và hiển thị ảnh từ URL API bằng Authorization header
const ChatImageFromUrl = ({ url }: { url: string }) => {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [error, setError] = useState(false);
  useEffect(() => {
    let cancelled = false;
    async function fetchImg() {
      setError(false);
      try {
        const token =
          typeof window !== "undefined"
            ? localStorage.getItem(authConfig.TOKEN_KEY)
            : null;
        const isLocalApi = isSameApiOrigin(url);
        const headers: HeadersInit = {};
        if (token && isLocalApi) {
          headers["Authorization"] = `Bearer ${token}`;
        }
        const res = await fetch(url, { headers, credentials: "include" });
        if (!res.ok) throw new Error("fetch error");
        const blob = await res.blob();
        if (!cancelled) setBlobUrl(URL.createObjectURL(blob));
      } catch {
        if (!cancelled) setError(true);
      }
    }
    fetchImg();
    return () => {
      cancelled = true;
      if (blobUrl) URL.revokeObjectURL(blobUrl);
    };
    // eslint-disable-next-line
  }, [url]);
  if (error)
    return <div className="text-xs text-red-500">无法加载图片</div>;
  if (!blobUrl)
    return <div className="w-32 h-24 bg-gray-200 animate-pulse rounded" />;
  return (
    <img
      src={blobUrl}
      alt="聊天图片"
      className="w-full h-full rounded shadow border border-gray-200 dark:border-gray-700 object-contain"
      style={{ width: "100%", height: "100%" }}
    />
  );
};
import { Card, CardContent } from "@/ui/card";
import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import { ScrollArea } from "@/ui/scroll-area";
import { Avatar, AvatarFallback } from "@/ui/avatar";
import { Badge } from "@/ui/badge";
import {
  Send,
  Bot,
  User,
  Loader2,
  Trash2,
  Copy,
  Check,
  Download,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { toast } from "sonner";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeHighlight from "rehype-highlight";
import type { Components } from "react-markdown";
// Custom markdown components for react-markdown v8+
const markdownComponents: Components = {
  a: (props) => (
    <a
      {...props}
      target="_blank"
      rel="noopener noreferrer"
      className="text-primary underline hover:text-primary/80 transition-colors"
      style={{}}
    />
  ),
  code: (props: { inline?: boolean; children?: React.ReactNode }) => {
    const { inline, children, ...rest } = props;
    return inline ? (
      <code
        {...rest}
        className="bg-muted text-foreground rounded px-1.5 py-0.5 text-[13px]"
      >
        {children}
      </code>
    ) : (
      <pre className="bg-[oklch(0.2_0.02_260)] text-[oklch(0.95_0.01_250)] rounded-lg p-3 overflow-x-auto my-2">
        <code>{children}</code>
      </pre>
    );
  },
  img: (props) => {
    const src = (props as { src?: string }).src;
    if (src && isSameApiOrigin(src)) {
      return <ChatImageFromUrl url={src} />;
    }
    return (
      <img
        {...props}
        style={{ maxWidth: "100%", borderRadius: 8, margin: "8px 0" }}
        alt="Markdown img"
      />
    );
  },
  ul: (props) => <ul {...props} className="pl-5 my-2 list-disc" />,
  ol: (props) => <ol {...props} className="pl-5 my-2 list-decimal" />,
  li: (props) => <li {...props} className="mb-1" />,
  blockquote: (props) => (
    <blockquote
      {...props}
      className="border-l-4 border-primary bg-muted/50 px-4 py-2 my-2 rounded text-foreground/80"
    />
  ),
  p: (props) => <p {...props} className="my-2" />,
};
import { useWebSocket } from "../../../../hooks/useWebSocket";

// Helper function để tạo unique message ID với random string
const generateMessageId = () => {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 15);
  return `msg_${timestamp}_${random}`;
};

interface VehicleData {
  count_car: number;
  count_motor: number;
  speed_car: number;
  speed_motor: number;
}

interface TrafficData {
  [roadName: string]: VehicleData;
}

interface Message {
  id: string;
  text: string;
  user: boolean;
  time: string;
  typing?: boolean;
  image?: string[];
}

interface ChatInterfaceProps {
  trafficData: TrafficData;
}

// Memoized MessageBubble component để tránh re-render không cần thiết
const MessageBubble = memo(
  ({
    msg,
    copiedMessageId,
    onCopyMessage,
    onPreviewImage,
  }: {
    msg: Message;
    copiedMessageId: string | null;
    onCopyMessage: (text: string, id: string) => void;
    onPreviewImage: (url: string) => void;
  }) => {
    return (
      <motion.div
        key={msg.id}
        initial={{ opacity: 0, y: 20, scale: 0.95 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: -10, scale: 0.95 }}
        transition={{
          duration: 0.3,
          ease: "easeOut",
        }}
        className={`flex ${msg.user ? "justify-end" : "justify-start"}`}
      >
        <div
          className={`${
            msg.image && msg.image.length > 0
              ? "max-w-[90%] sm:max-w-[60%] md:max-w-[45%]"
              : "max-w-[60%] sm:max-w-[45%] md:max-w-[35%]"
          } flex flex-col gap-1 rounded-2xl px-4 py-3 shadow-sm border text-base transition-all hover:shadow-md ${
            msg.user
              ? "bg-primary/10 dark:bg-primary/15 border-primary/20 text-right ml-auto"
              : "bg-card border-border/50 text-left mr-auto"
          }`}
        >
          <div className="flex items-center gap-2 mb-1">
            <Avatar className="w-6 h-6">
              {msg.user ? (
                <AvatarFallback>
                  <User className="w-4 h-4" />
                </AvatarFallback>
              ) : (
                <AvatarFallback>
                  <Bot className="w-4 h-4" />
                </AvatarFallback>
              )}
            </Avatar>
            <span className="text-xs text-muted-foreground">
              {msg.user ? "你" : "AI"}
            </span>
            <span className="text-xs text-muted-foreground/60 ml-2">{msg.time}</span>
            {msg.typing && (
              <Loader2 className="w-4 h-4 animate-spin text-primary ml-2" />
            )}
          </div>
          {msg.image && msg.image.length > 0 && (
            <div className="flex flex-wrap gap-3 mb-2">
              {msg.image.map((imgUrl, i) => (
                <button
                  key={i}
                  type="button"
                  className="w-full sm:max-w-[520px] h-auto hover:opacity-90 transition"
                  onClick={() => onPreviewImage(imgUrl)}
                  title="查看大图"
                >
                  <ChatImageFromUrl url={imgUrl} />
                </button>
              ))}
            </div>
          )}
          {msg.text && (
            <ReactMarkdown
              components={markdownComponents}
              remarkPlugins={[remarkGfm]}
              rehypePlugins={[rehypeHighlight]}
            >
              {msg.text}
            </ReactMarkdown>
          )}
          <div className="flex gap-2 mt-1">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => onCopyMessage(msg.text, msg.id)}
              title="复制内容"
              className="p-1"
            >
              {copiedMessageId === msg.id ? (
                <Check className="w-4 h-4 text-green-500" />
              ) : (
                <Copy className="w-4 h-4" />
              )}
            </Button>
            {msg.user && (
              <Badge variant="outline" className="text-xs">
                你
              </Badge>
            )}
            {!msg.user && (
              <Badge variant="secondary" className="text-xs">
                AI
              </Badge>
            )}
          </div>
        </div>
      </motion.div>
    );
  },
  // Custom comparison function để optimize re-renders
  (prevProps, nextProps) => {
    return (
      prevProps.msg.id === nextProps.msg.id &&
      prevProps.msg.text === nextProps.msg.text &&
      prevProps.msg.typing === nextProps.msg.typing &&
      prevProps.copiedMessageId === nextProps.copiedMessageId &&
      JSON.stringify(prevProps.msg.image) ===
        JSON.stringify(nextProps.msg.image)
    );
  }
);
MessageBubble.displayName = "MessageBubble";

// extractImageLinks and removeImageLinksFromText are unused, removed for lint clean
function addTokenToImageUrl(url: string): string {
  if (!url) return url;

  // Fix double protocol issue (http://http://)
  url = url.replace(/^https?:\/\/https?:\/\//i, (match) => {
    // Keep only the first protocol
    return match.includes("https://https://") ? "https://" : "http://";
  });

  // Remove all existing token parameters first
  url = url.replace(/([?&])token=[^&]+/g, "");
  // Clean up trailing & or ?
  url = url.replace(/[?&]$/, "");

  // Only add token if it's a local API URL
  if (url.includes("localhost:8000") || url.includes("127.0.0.1:8000")) {
    const token = localStorage.getItem(authConfig.TOKEN_KEY);
    if (token) {
      const separator = url.includes("?") ? "&" : "?";
      return `${url}${separator}token=${encodeURIComponent(token)}`;
    }
  }
  return url;
}

function processImageUrlsInText(text: string): string {
  if (!text) return text;

  // Match image URLs
  const imgRegex =
    /(https?:\/\/[\w\-./%?=&@]+\.(?:jpg|jpeg|png|webp|gif|bmp))(?!\S)/gi;

  return text.replace(imgRegex, (match) => {
    return addTokenToImageUrl(match);
  });
}

const ChatInterface = ({ trafficData }: ChatInterfaceProps) => {
  // Load chat history from localStorage using helper functions
  const [messages, setMessages] = useState<Message[]>(() => loadChatHistory());
  const [input, setInput] = useState(() => loadChatDraft());
  const [previewImage, setPreviewImage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [copiedMessageId, setCopiedMessageId] = useState<string | null>(null);
  const scrollAreaRef = useRef<HTMLDivElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Hàm scroll xuống cuối
  const scrollToBottom = useCallback(() => {
    // Sử dụng setTimeout để đảm bảo DOM đã update xong
    setTimeout(() => {
      messagesEndRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "end",
      });
    }, 100);
  }, []);

  // Track current token to reload messages when user switches accounts
  const [currentToken, setCurrentToken] = useState(() =>
    localStorage.getItem(authConfig.TOKEN_KEY)
  );

  // Reload messages when token changes (user logged in/out or switched accounts)
  useEffect(() => {
    const token = localStorage.getItem(authConfig.TOKEN_KEY);

    if (token !== currentToken) {
      console.log("[ChatInterface] Token changed, reloading messages");
      console.log("  Old token:", currentToken?.substring(0, 10));
      console.log("  New token:", token?.substring(0, 10));

      setCurrentToken(token);
      const newMessages = loadChatHistory();
      setMessages(newMessages);
      setInput(loadChatDraft());

      console.log(
        "[ChatInterface] Loaded",
        newMessages.length,
        "messages for new user"
      );
    }
  }, [currentToken]);

  // Check token periodically in case user logs in/out in another tab
  useEffect(() => {
    const interval = setInterval(() => {
      const token = localStorage.getItem(authConfig.TOKEN_KEY);
      if (token !== currentToken) {
        setCurrentToken(token);
      }
    }, 1000); // Check every second

    return () => clearInterval(interval);
  }, [currentToken]);

  // Save chat history to localStorage whenever messages change
  useEffect(() => {
    saveChatHistory(messages);
  }, [messages]);

  // Persist draft input
  useEffect(() => {
    saveChatDraft(input);
  }, [input]);

  // Kiểm tra trafficData
  useEffect(() => {
    const hasData = trafficData && Object.keys(trafficData).length > 0;
    console.log("Traffic Data Status:", {
      hasData,
      roads: Object.keys(trafficData || {}),
      data: trafficData,
    });

      if (!hasData && messages.length === 1) {
        // Chỉ hiển thị thông báo nếu là tin nhắn chào đầu tiên
        setMessages([
          {
            id: "1",
            text: "你好！我是智能交通系统的 AI 助手。目前系统正在启动并采集交通数据，一旦有道路信息我会第一时间告知。",
            user: false,
            time: new Date().toLocaleTimeString("zh-CN"),
          },
        ]);
      }
    }, [trafficData, messages.length]);

  // Auto-scroll khi messages thay đổi (gửi tin mới hoặc nhận tin mới)
  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  // Scroll xuống cuối khi component mount (mở chat lần đầu)
  useEffect(() => {
    scrollToBottom();
  }, [scrollToBottom]);

  // Chat WebSocket with authentication - setup trước để dùng trong handlers
  const token = localStorage.getItem(authConfig.TOKEN_KEY);
  const chatWsUrl = endpoints.chatWs;

  // Show message if no token
  useEffect(() => {
    if (!token) {
      toast.error("请登录后使用 AI 聊天");
    }
  }, [token]);

  const {
    data: chatData,
    send: chatSocketSend,
    isConnected: isWsConnected,
    error: wsError,
  } = useWebSocket(chatWsUrl, {
    reconnectInterval: 3000,
    maxReconnectAttempts: 5,
    authToken: token,
  });

  useEffect(() => {
    if (wsError) {
      console.error("WebSocket Error:", wsError);
      // Only show error toast if it's a final error, not retry messages
      if (
        wsError.includes("多次重试后仍无法连接服务器") ||
        wsError.includes("无法创建 WebSocket 连接")
      ) {
        toast.error(wsError);
      }
    }
  }, [wsError]);

  // Monitor connection status
  useEffect(() => {
    console.log("WebSocket Connection Status:", isWsConnected);
    if (isWsConnected) {
      toast.success("AI 连接成功！");
    }
  }, [isWsConnected]);

  // Bỏ phần xử lý/biến đổi câu hỏi - gửi thẳng nội dung người dùng nhập

  // Memoize handlers để tránh re-create functions
  const handleSendMessage = useCallback(async () => {
    if (!input.trim() || isLoading) return;

    const userMessage = input.trim();
    console.log("Sending message:", { message: userMessage }); // Log tin nhắn gửi đi
    setInput("");
    // clear saved draft after sending
    clearChatDraft();
    setIsLoading(true);

    // Add user message
    const userMsg: Message = {
      id: generateMessageId(),
      text: userMessage,
      user: true,
      time: new Date().toLocaleTimeString("zh-CN"),
    };
    setMessages((prev) => [...prev, userMsg]);

    // Scroll xuống sau khi thêm tin nhắn người dùng
    scrollToBottom();

    // Add typing indicator
    const typingMsg: Message = {
      id: "typing",
      text: "",
      user: false,
      time: "",
      typing: true,
    };
    setMessages((prev) => [...prev, typingMsg]);

    try {
      if (!isWsConnected) {
        setMessages((prev) => [
          ...prev.filter((msg) => msg.id !== "typing"),
          {
            id: generateMessageId(),
            text: "无法连接到 AI，请稍后重试。",
            user: false,
            time: new Date().toLocaleTimeString("zh-CN"),
          },
        ]);
        toast.error("无法连接到 AI");
        setIsLoading(false);
        inputRef.current?.focus();
        return;
      }

      // Gửi thẳng tin nhắn người dùng tới AI
      const ok = chatSocketSend({ message: userMessage });
      console.log("Message sent status:", ok);

      if (!ok) {
        setMessages((prev) => [
          ...prev.filter((msg) => msg.id !== "typing"),
          {
            id: generateMessageId(),
            text: "无法发送消息到 AI，请重试。",
            user: false,
            time: new Date().toLocaleTimeString("zh-CN"),
          },
        ]);
        toast.error("无法发送消息到 AI");
        setIsLoading(false);
        inputRef.current?.focus();
      }
    } catch (error) {
      console.error("Chat error:", error);

      // Remove typing indicator and add error message
      setMessages((prev) => [
        ...prev.filter((msg) => msg.id !== "typing"),
        {
          id: generateMessageId(),
          text: "发送消息时发生错误，请重试。",
          user: false,
          time: new Date().toLocaleTimeString("zh-CN"),
        },
      ]);

      toast.error("无法连接到 AI");
      setIsLoading(false);
      inputRef.current?.focus();
    }
  }, [input, isLoading, isWsConnected, chatSocketSend, scrollToBottom]); // Dependencies for useCallback

  useEffect(() => {
    if (!chatData) return;
    try {
      // Log toàn bộ dữ liệu nhận được từ WebSocket
      console.log("WebSocket Raw Response:", chatData);
      const payload = chatData as
        | { message?: string; image?: string[] }
        | undefined;
      const responseText = payload?.message;
      const responseImage = payload?.image;

      // Log chi tiết từng phần của response
      console.log("Response Text:", responseText);
      console.log("Response Images:", responseImage);

      // Chỉ bỏ qua nếu cả text và image đều không có hoặc undefined
      // Chấp nhận empty string vì AI có thể gửi text rỗng kèm ảnh
      const hasText = responseText !== undefined && responseText !== null;
      const hasImages = responseImage && responseImage.length > 0;

      if (!hasText && !hasImages) {
        console.log("No text or images in response, skipping");
        setMessages((prev) => prev.filter((msg) => msg.id !== "typing"));
        setIsLoading(false);
        inputRef.current?.focus();
        return;
      }

      // Process image URLs to add authentication token
      const imageUrls = (responseImage || []).map((url) =>
        addTokenToImageUrl(url)
      );

      // Process text to add authentication token to any image URLs in text
      const processedText = processImageUrlsInText(responseText ?? "");

      console.log("Processed Response:", {
        text: processedText,
        images: imageUrls,
        hasText: !!processedText,
        hasImages: imageUrls.length > 0,
      });

      setMessages((prev) => {
        // Remove typing indicator
        const filtered = prev.filter((msg) => msg.id !== "typing");
        // Add AI response
        return [
          ...filtered,
          {
            id: generateMessageId(),
            text: processedText,
            user: false,
            time: new Date().toLocaleTimeString("zh-CN"),
            image: imageUrls,
          },
        ];
      });

      // Bỏ toast success notification
      // toast.success("Đã nhận được phản hồi từ AI");
    } catch (error) {
      console.error("Error processing WebSocket response:", error);
      toast.error("处理响应时出错");
    }
    setIsLoading(false);
    inputRef.current?.focus();
  }, [chatData]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        handleSendMessage();
      }
    },
    [handleSendMessage]
  );

  // Restore clearChat for delete button
  const clearChat = useCallback(() => {
    const welcomeMsg: Message = {
      id: "1",
      text: "你好！我是智能交通系统的 AI 助手。你可以询问当前路况、车辆统计，或任意正在监控道路的信息。我能帮你什么？",
      user: false,
      time: new Date().toLocaleTimeString("zh-CN"),
    };
    setMessages([welcomeMsg]);
    // Clear chat history using helper function
    clearChatHistory();
    toast.success("已清空聊天记录");
  }, []);

  const exportHistory = useCallback(() => {
    try {
      const dataStr = JSON.stringify(messages, null, 2);
      const blob = new Blob([dataStr], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `chat-history-${new Date()
        .toISOString()
        .slice(0, 10)}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      toast.success("已导出聊天记录");
    } catch (error) {
      console.error("Export error:", error);
      toast.error("无法导出聊天记录");
    }
  }, [messages]);

  const copyMessage = useCallback(async (text: string, messageId: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedMessageId(messageId);
      setTimeout(() => setCopiedMessageId(null), 1500);
      toast.success("已复制");
    } catch {
      toast.error("复制失败");
    }
  }, []);

  const handlePreviewImage = useCallback((url: string) => {
    setPreviewImage(url);
  }, []);

  // --- COMPONENT RETURN ---
  return (
    <Card className="h-[calc(100vh-8rem)] min-h-[600px] max-h-[900px] flex flex-col relative shadow-lg shadow-primary/5 border-border/50">
      {/* Floating action buttons */}
      <div className="absolute top-3 right-3 z-10 flex gap-2">
        <Button
          variant="ghost"
          size="icon"
          onClick={exportHistory}
          title="导出聊天记录"
          className="bg-card/90 backdrop-blur-sm hover:bg-primary/10 border border-border/50 shadow-sm hover:shadow-md transition-all"
        >
          <Download className="w-4 h-4 sm:w-5 sm:h-5 text-primary" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          onClick={clearChat}
          title="清空聊天记录"
          className="bg-card/90 backdrop-blur-sm hover:bg-destructive/10 border border-border/50 shadow-sm hover:shadow-md transition-all"
        >
          <Trash2 className="w-4 h-4 sm:w-5 sm:h-5 text-destructive" />
        </Button>
      </div>
      <CardContent className="flex-1 p-3 sm:p-6 overflow-hidden">
        <ScrollArea className="h-full w-full pr-4" ref={scrollAreaRef}>
          <div className="flex flex-col gap-3 sm:gap-4">
            <AnimatePresence initial={false}>
              {messages.map((msg) => (
                <MessageBubble
                  key={msg.id}
                  msg={msg}
                  copiedMessageId={copiedMessageId}
                  onCopyMessage={copyMessage}
                  onPreviewImage={handlePreviewImage}
                />
              ))}
            </AnimatePresence>
            {/* Invisible element để scroll xuống */}
            <div ref={messagesEndRef} className="h-1" />
          </div>
        </ScrollArea>
      </CardContent>
      <form
        className="flex items-center gap-2 sm:gap-3 p-3 sm:p-4 border-t border-border/50 bg-muted/30"
        onSubmit={(e) => {
          e.preventDefault();
          handleSendMessage();
        }}
      >
        <Input
          ref={inputRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="请输入交通相关问题..."
          className="flex-1 h-11 sm:h-12"
          disabled={isLoading}
        />
        <Button
          type="submit"
          variant="default"
          size="icon"
          disabled={isLoading || !input.trim()}
          title="发送"
          className="h-11 w-11 sm:h-12 sm:w-12 flex-shrink-0"
        >
          <Send className="w-4 h-4 sm:w-5 sm:h-5" />
        </Button>
      </form>
      {/* Simple image preview modal */}
      {previewImage && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.2 }}
          className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4 backdrop-blur-sm"
          onClick={() => setPreviewImage(null)}
        >
          <motion.img
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            transition={{ duration: 0.3, ease: "easeOut" }}
            src={previewImage}
            alt="Preview"
            className="max-w-full max-h-full object-contain rounded-lg shadow-2xl"
          />
        </motion.div>
      )}
    </Card>
  );
};
export default ChatInterface;
