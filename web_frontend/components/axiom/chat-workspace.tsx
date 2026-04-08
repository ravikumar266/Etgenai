'use client'

import { useState, useEffect, useRef } from 'react'
import { Send, Paperclip, Bug } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { Spinner } from '@/components/ui/spinner'
import { EmailApprovalCard } from './email-approval-card'
import { MarkdownRenderer } from './markdown-renderer'
import { cn } from '@/lib/utils'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  toolsUsed?: string[]
}

interface PendingEmail {
  id: string
  to: string
  subject: string
  body: string
}

interface ChatWorkspaceProps {
  messages: Message[]
  pendingEmail: PendingEmail | null
  isLoading: boolean
  onSendMessage: (message: string) => void
  onApproveEmail: () => void
  onDenyEmail: () => void
}

export function ChatWorkspace({
  messages,
  pendingEmail,
  isLoading,
  onSendMessage,
  onApproveEmail,
  onDenyEmail,
}: ChatWorkspaceProps) {
  const [input, setInput] = useState('')
  const scrollRef = useRef<HTMLDivElement>(null)

  // Auto-scroll to bottom
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages, pendingEmail])

  const handleSend = () => {
    if (!input.trim()) return
    onSendMessage(input)
    setInput('')
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && e.ctrlKey) {
      handleSend()
    }
  }

  const formatToolName = (tool: string) => {
    return tool
      .replace(/_/g, ' ')
      .split(' ')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ')
  }

  return (
    <div className="flex-1 flex flex-col h-screen bg-background">
      {/* Top Bar */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-stone-300 dark:border-stone-700">
        <h2 className="text-2xl font-serif font-bold text-foreground">Chat</h2>
        <Button variant="ghost" size="icon" className="text-muted-foreground hover:text-foreground">
          <Bug className="w-5 h-5" />
        </Button>
      </div>

      {/* Messages Area */}
      <ScrollArea className="flex-1 p-6">
        <div className="max-w-4xl mx-auto space-y-4">
          {messages.length === 0 ? (
            <div className="h-96 flex flex-col items-center justify-center text-center">
              <div className="w-12 h-12 rounded border border-stone-300 dark:border-stone-700 flex items-center justify-center mb-4 bg-stone-50 dark:bg-stone-950">
                <span className="text-2xl">✨</span>
              </div>
              <h3 className="text-2xl font-serif font-bold text-foreground mb-2">
                Start a new conversation
              </h3>
              <p className="text-muted-foreground max-w-xs">
                Ask me anything. I can help with analysis, writing, coding, and more.
              </p>
            </div>
          ) : (
            <>
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={cn(
                    'flex gap-4 animate-in fade-in-50 duration-300',
                    message.role === 'user' ? 'justify-end' : 'justify-start'
                  )}
                >
                  {message.role === 'assistant' && (
                    <div className="w-8 h-8 rounded border border-stone-300 dark:border-stone-700 flex items-center justify-center flex-shrink-0 mt-1 bg-stone-50 dark:bg-stone-950">
                      <span className="text-lg">⚙️</span>
                    </div>
                  )}

                  <div
                    className={cn(
                      'flex-1 max-w-2xl',
                      message.role === 'user'
                        ? 'flex justify-end'
                        : 'flex justify-start'
                    )}
                  >
                    <div
                      className={cn(
                        'rounded px-4 py-3 break-words',
                        message.role === 'user'
                          ? 'bg-primary text-primary-foreground rounded-br-none border border-stone-300 dark:border-stone-700'
                          : 'bg-card text-card-foreground border border-stone-300 dark:border-stone-700 rounded-bl-none'
                      )}
                    >
                      {message.role === 'user' ? (
                        <p className="text-sm leading-relaxed whitespace-pre-wrap">{message.content}</p>
                      ) : (
                        <div className="text-sm leading-relaxed">
                          <MarkdownRenderer content={message.content} />
                        </div>
                      )}

                      {message.toolsUsed && message.toolsUsed.length > 0 && (
                        <div className="flex flex-wrap gap-2 mt-3 pt-3 border-t border-current/10">
                          <span className="text-xs opacity-70">Tools used:</span>
                          {message.toolsUsed.map((tool) => (
                            <Badge
                              key={tool}
                              variant="secondary"
                              className={cn(
                                'text-xs',
                                message.role === 'user'
                                  ? 'bg-accent-foreground/20 text-accent-foreground'
                                  : 'bg-muted text-muted-foreground'
                              )}
                            >
                              {formatToolName(tool)}
                            </Badge>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>

                  {message.role === 'user' && (
                    <div className="w-8 h-8 rounded border border-stone-300 dark:border-stone-700 flex items-center justify-center flex-shrink-0 mt-1 bg-stone-50 dark:bg-stone-950">
                      <span className="text-lg">👤</span>
                    </div>
                  )}
                </div>
              ))}

              {/* Pending Email */}
              {pendingEmail && (
                <div className="flex gap-4 animate-in fade-in-50 duration-300">
                  <div className="w-8 h-8 rounded border border-stone-300 dark:border-stone-700 flex items-center justify-center flex-shrink-0 mt-1 bg-stone-50 dark:bg-stone-950">
                    <span className="text-lg">📧</span>
                  </div>
                  <div className="flex-1 max-w-2xl">
                    <EmailApprovalCard
                      email={pendingEmail}
                      onApprove={onApproveEmail}
                      onDeny={onDenyEmail}
                    />
                  </div>
                </div>
              )}

              {/* Loading Indicator */}
              {isLoading && (
                <div className="flex gap-4">
                  <div className="w-8 h-8 rounded border border-stone-300 dark:border-stone-700 flex items-center justify-center flex-shrink-0 bg-stone-50 dark:bg-stone-950">
                    <Spinner className="w-4 h-4 text-primary" />
                  </div>
                  <div className="flex-1 max-w-2xl">
                    <div className="bg-card text-card-foreground border border-stone-300 dark:border-stone-700 rounded rounded-bl-none px-4 py-3">
                      <div className="flex gap-2">
                        <div className="w-2 h-2 rounded-full bg-accent animate-bounce" />
                        <div
                          className="w-2 h-2 rounded-full bg-accent animate-bounce"
                          style={{ animationDelay: '0.1s' }}
                        />
                        <div
                          className="w-2 h-2 rounded-full bg-accent animate-bounce"
                          style={{ animationDelay: '0.2s' }}
                        />
                      </div>
                    </div>
                  </div>
                </div>
              )}

              <div ref={scrollRef} />
            </>
          )}
        </div>
      </ScrollArea>

      {/* Input Area */}
      <div className="border-t border-stone-300 dark:border-stone-700 bg-background p-6">
        <div className="max-w-4xl mx-auto">
          <div className="relative flex gap-3">
            <div className="flex-1 relative">
              <Textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Type your message here... (Ctrl+Enter to send)"
                className="min-h-12 max-h-48 resize-none bg-card border-stone-300 dark:border-stone-700 border text-foreground placeholder:text-muted-foreground focus-visible:ring-primary"
                disabled={isLoading}
              />
              <Button
                size="icon"
                variant="ghost"
                className="absolute right-2 bottom-2 text-muted-foreground hover:text-foreground"
              >
                <Paperclip className="w-5 h-5" />
              </Button>
            </div>
            <Button
              onClick={handleSend}
              disabled={isLoading || !input.trim()}
              className="h-12 bg-primary text-primary-foreground hover:bg-primary/90"
            >
              {isLoading ? (
                <Spinner className="w-4 h-4" />
              ) : (
                <Send className="w-5 h-5" />
              )}
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
