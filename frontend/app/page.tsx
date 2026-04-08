"use client";

import { useState, useEffect } from "react";
import { toast } from "sonner";
import { Sidebar } from "@/components/axiom/sidebar";
import { ChatWorkspace } from "@/components/axiom/chat-workspace";
import { KnowledgeBaseModal } from "@/components/axiom/knowledge-base-modal";
import {
  sendMessage,
  getThreads,
  getThreadMessages,
  approveEmail,
  rejectEmail,
} from "@/lib/api";

interface Thread {
  id: string;
  title: string;
  timestamp: number;
}

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  toolsUsed?: string[];
}

interface PendingEmail {
  id: string;
  to: string;
  subject: string;
  body: string;
}

export default function Home() {
  const [threads, setThreads] = useState<Thread[]>([]);
  const [currentThreadId, setCurrentThreadId] = useState<string | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [pendingEmail, setPendingEmail] = useState<PendingEmail | null>(null);
  const [isLoadingMessage, setIsLoadingMessage] = useState(false);
  const [showKnowledgeBase, setShowKnowledgeBase] = useState(false);
  const [systemHealth, setSystemHealth] = useState<"healthy" | "warning">(
    "healthy",
  );

  // Load threads on mount
  useEffect(() => {
    const loadThreads = async () => {
      try {
        const threadsList = await getThreads();
        const formattedThreads = threadsList.map((t: any) => ({
          id: t.thread_id,
          title: t.title || "Untitled",
          timestamp: new Date(t.created_at).getTime(),
        }));
        setThreads(formattedThreads);

        if (formattedThreads.length > 0) {
          setCurrentThreadId(formattedThreads[0].id);
          const threadMessages = await getThreadMessages(
            formattedThreads[0].id,
          );
          setMessages(
            threadMessages.map((m: any, idx: number) => ({
              id: `msg-${idx}`,
              role: m.role,
              content: m.content,
            })),
          );
        }
      } catch (error) {
        console.log("[v0] Failed to load threads:", error);
        setSystemHealth("warning");
      }
    };
    loadThreads();
  }, []);

  const handleNewChat = async () => {
    setCurrentThreadId(null);
    setMessages([]);
    setPendingEmail(null);
  };

  const handleSelectThread = async (threadId: string) => {
    setCurrentThreadId(threadId);
    try {
      const threadMessages = await getThreadMessages(threadId);
      setMessages(
        threadMessages.map((m: any, idx: number) => ({
          id: `msg-${idx}`,
          role: m.role,
          content: m.content,
        })),
      );
    } catch (error) {
      console.log("[v0] Failed to load thread messages:", error);
      toast.error("Failed to load messages");
    }
    setPendingEmail(null);
  };

  const handleSendMessage = async (content: string) => {
    // Add user message
    const userMsg: Message = {
      id: `msg-${Date.now()}`,
      role: "user",
      content,
    };
    setMessages((prev) => [...prev, userMsg]);
    setIsLoadingMessage(true);

    try {
      const response = await sendMessage(currentThreadId || null, content);
      const assistantMsg: Message = {
        id: `msg-${Date.now()}`,
        role: "assistant",
        content: response.reply,
        toolsUsed: response.tools_used || [
          "api_integration",
          "context_aware_response",
        ],
      };
      setMessages((prev) => [...prev, assistantMsg]);

      // Update thread ID if new
      if (!currentThreadId && response.thread_id) {
        setCurrentThreadId(response.thread_id);
      }
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Failed to send message";
      toast.error(message);
      const errorMsg: Message = {
        id: `msg-${Date.now()}`,
        role: "assistant",
        content: `Error: ${message}. Please try again.`,
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsLoadingMessage(false);
    }
  };

  const handleApproveEmail = async () => {
    if (!pendingEmail) return;
    try {
      await approveEmail(pendingEmail.id);
      setPendingEmail(null);
      const successMsg: Message = {
        id: `msg-${Date.now()}`,
        role: "assistant",
        content: `✓ Email sent successfully to ${pendingEmail.to}`,
      };
      setMessages((prev) => [...prev, successMsg]);
      toast.success("Email approved and sent");
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Failed to approve email";
      toast.error(message);
    }
  };

  const handleDenyEmail = async () => {
    if (!pendingEmail) return;
    try {
      await rejectEmail(pendingEmail.id);
      setPendingEmail(null);
      const msg: Message = {
        id: `msg-${Date.now()}`,
        role: "assistant",
        content:
          "Email draft discarded. Let me know if you'd like to revise it.",
      };
      setMessages((prev) => [...prev, msg]);
      toast.success("Email rejected");
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Failed to reject email";
      toast.error(message);
    }
  };

  return (
    <div className="flex h-screen bg-background text-foreground">
      <Sidebar
        threads={threads}
        currentThreadId={currentThreadId}
        onNewChat={handleNewChat}
        onSelectThread={handleSelectThread}
        onSettingsClick={() => setShowKnowledgeBase(true)}
        systemHealth={systemHealth}
      />

      <ChatWorkspace
        messages={messages}
        pendingEmail={pendingEmail}
        isLoading={isLoadingMessage}
        onSendMessage={handleSendMessage}
        onApproveEmail={handleApproveEmail}
        onDenyEmail={handleDenyEmail}
      />

      {showKnowledgeBase && (
        <KnowledgeBaseModal onClose={() => setShowKnowledgeBase(false)} />
      )}
    </div>
  );
}
