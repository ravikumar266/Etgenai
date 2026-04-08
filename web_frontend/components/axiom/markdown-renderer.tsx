"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface MarkdownRendererProps {
  content: string;
}

export function MarkdownRenderer({ content }: MarkdownRendererProps) {
  if (!content) return null;

  return (
    <div className="whitespace-pre-wrap break-words text-sm leading-relaxed space-y-2 prose dark:prose-invert max-w-none">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          code({ node, inline, className, children, ...props }: any) {
            return (
              <code
                className={`${className} bg-stone-100 px-1 py-0.5 rounded text-sm font-mono border border-stone-300 text-stone-900`}
                {...props}
              >
                {children}
              </code>
            );
          },
          a({ node, children, ...props }: any) {
            return (
              <a
                target="_blank"
                className="text-primary hover:underline"
                {...props}
              >
                {children}
              </a>
            );
          },
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
