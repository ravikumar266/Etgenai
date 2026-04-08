"use client";

import { useState } from "react";
import { Plus, MessageSquare, Settings, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import { ThemeToggle } from "./theme-toggle";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";

interface Thread {
  id: string;
  title: string;
  timestamp: number;
}

interface SidebarProps {
  threads: Thread[];
  currentThreadId: string | null;
  onNewChat: () => void;
  onSelectThread: (threadId: string) => void;
  onSettingsClick: () => void;
  systemHealth: "healthy" | "warning";
}

export function Sidebar({
  threads,
  currentThreadId,
  onNewChat,
  onSelectThread,
  onSettingsClick,
  systemHealth,
}: SidebarProps) {
  const [isSystemExpanded, setIsSystemExpanded] = useState(false);

  const formatDate = (timestamp: number) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) return "Today";
    if (days === 1) return "Yesterday";
    if (days < 7) return `${days}d ago`;
    return date.toLocaleDateString();
  };

  return (
    <div className="flex flex-col h-screen w-64 bg-sidebar border-r border-sidebar-border">
      {/* Header */}
      <div className="flex flex-col gap-3 p-4 border-b border-stone-300 dark:border-stone-700">
        <h1 className="text-2xl font-serif font-bold text-sidebar-foreground">
          Axiom
        </h1>
        <Button
          onClick={onNewChat}
          className="w-full bg-primary text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="w-4 h-4 mr-2" />
          New Chat
        </Button>
      </div>

      {/* Chat History */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <div className="px-4 py-3 border-b border-stone-300 dark:border-stone-700">
          <h2 className="text-xs font-semibold text-sidebar-foreground/70 uppercase tracking-wider">
            Conversations
          </h2>
        </div>
        <ScrollArea className="flex-1">
          <div className="px-2 pb-4 space-y-1">
            {threads.map((thread) => (
              <button
                key={thread.id}
                onClick={() => onSelectThread(thread.id)}
                className={cn(
                  "w-full text-left px-3 py-2 rounded text-sm transition-colors border border-transparent",
                  currentThreadId === thread.id
                    ? "bg-accent text-accent-foreground border-stone-300 dark:border-stone-700"
                    : "text-sidebar-foreground hover:bg-stone-100 dark:hover:bg-stone-900",
                )}
              >
                <div className="flex items-center gap-2">
                  <MessageSquare className="w-4 h-4 shrink-0" />
                  <div className="flex-1 min-w-0">
                    <p className="truncate font-medium">{thread.title}</p>
                    <p className="text-xs opacity-60">
                      {formatDate(thread.timestamp)}
                    </p>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </ScrollArea>
      </div>

      {/* System Status & Debug */}
      <div className="border-t border-stone-300 dark:border-stone-700 p-3 space-y-2">
        <div className="flex items-center gap-2 px-2 py-1">
          <ThemeToggle />
        </div>

        <Collapsible open={isSystemExpanded} onOpenChange={setIsSystemExpanded}>
          <CollapsibleTrigger asChild>
            <button className="w-full flex items-center justify-between px-3 py-2 rounded hover:bg-stone-100 dark:hover:bg-stone-900 transition-colors text-sm text-sidebar-foreground">
              <div className="flex items-center gap-2">
                <div
                  className={cn(
                    "w-2 h-2 rounded-full",
                    systemHealth === "healthy" ? "bg-success" : "bg-yellow-500",
                  )}
                />
                <span className="font-medium">System Status</span>
              </div>
              <span className="text-xs">{isSystemExpanded ? "−" : "+"}</span>
            </button>
          </CollapsibleTrigger>
          <CollapsibleContent className="pt-2 space-y-2">
            <TooltipProvider>
              <div className="px-3 py-2 text-xs text-sidebar-foreground/70 bg-stone-100 dark:bg-stone-900 border border-stone-300 dark:border-stone-700 rounded">
                <Tooltip>
                  <TooltipTrigger asChild>
                    <div className="flex items-center gap-2 cursor-help">
                      {systemHealth === "healthy" ? (
                        <span>All systems operational</span>
                      ) : (
                        <>
                          <AlertCircle className="w-3 h-3" />
                          <span>Minor issues detected</span>
                        </>
                      )}
                    </div>
                  </TooltipTrigger>
                  <TooltipContent side="right">
                    <p>Last checked: just now</p>
                    <p>Background jobs: running</p>
                  </TooltipContent>
                </Tooltip>
              </div>

              <Button
                onClick={onSettingsClick}
                variant="ghost"
                size="sm"
                className="w-full justify-start text-sidebar-foreground/70 hover:text-sidebar-foreground hover:bg-sidebar-accent/20"
              >
                <Settings className="w-4 h-4 mr-2" />
                Knowledge Base
              </Button>
            </TooltipProvider>
          </CollapsibleContent>
        </Collapsible>
      </div>
    </div>
  );
}
