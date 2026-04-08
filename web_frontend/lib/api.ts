const BASE_URL = "/api";

export interface Message {
  role: "user" | "assistant";
  content: string;
}

export interface ChatResponse {
  reply: string;
  thread_id: string;
  tools_used?: string[];
  pending_email?: any;
  email_approval_required?: boolean;
}

export interface ThreadInfo {
  thread_id: string;
  title: string;
  created_at: string;
}

export interface UploadResponse {
  success: boolean;
  message: string;
  file_id?: string;
}

export interface KnowledgeBaseItem {
  id: string;
  name: string;
  type: string;
  created_at: string;
}

export interface EmailApproval {
  id: string;
  subject: string;
  body: string;
  recipient: string;
  status: "pending" | "approved" | "rejected";
}

// Chat endpoints
export async function sendMessage(
  threadId: string | null,
  message: string,
): Promise<ChatResponse> {
  const endpoint = threadId
    ? `${BASE_URL}/chat?thread_id=${encodeURIComponent(threadId)}`
    : `${BASE_URL}/chat`;

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      message,
    }),
  });

  if (!response.ok) {
    throw new Error(`Chat failed: ${response.statusText}`);
  }

  return response.json();
}

export async function getThreads(): Promise<ThreadInfo[]> {
  const response = await fetch(`${BASE_URL}/threads`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch threads: ${response.statusText}`);
  }

  return response.json();
}

export async function getThreadMessages(threadId: string): Promise<Message[]> {
  const response = await fetch(
    `${BASE_URL}/threads/${encodeURIComponent(threadId)}`,
    {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    },
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch messages: ${response.statusText}`);
  }

  return response.json();
}

// File upload endpoints
export async function uploadFile(file: File): Promise<UploadResponse> {
  // 50MB limit
  const MAX_SIZE = 50 * 1024 * 1024;
  if (file.size > MAX_SIZE) {
    throw new Error("File size must be less than 50MB");
  }

  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${BASE_URL}/upload-pdf`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    throw new Error(`Upload failed: ${response.statusText}`);
  }

  return response.json();
}

// Knowledge base endpoints
export async function getKnowledgeBase(): Promise<KnowledgeBaseItem[]> {
  const response = await fetch(`${BASE_URL}/knowledge-base`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch knowledge base: ${response.statusText}`);
  }

  return response.json();
}

export async function deleteKnowledgeItem(itemId: string): Promise<void> {
  const response = await fetch(
    `${BASE_URL}/knowledge-base/${encodeURIComponent(itemId)}`,
    {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
      },
    },
  );

  if (!response.ok) {
    throw new Error(`Delete failed: ${response.statusText}`);
  }
}

export async function testKnowledgeBase(query: string): Promise<string> {
  const response = await fetch(`${BASE_URL}/knowledge-base/test`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      query,
    }),
  });

  if (!response.ok) {
    throw new Error(`Test failed: ${response.statusText}`);
  }

  const data = await response.json();
  return data.result || data.response;
}

// Email approval endpoints
export async function getEmailApprovals(): Promise<EmailApproval[]> {
  const response = await fetch(`${BASE_URL}/email-approvals`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch emails: ${response.statusText}`);
  }

  return response.json();
}

export async function approveEmail(emailId: string): Promise<void> {
  const response = await fetch(
    `${BASE_URL}/email-approvals/${encodeURIComponent(emailId)}/approve`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
    },
  );

  if (!response.ok) {
    throw new Error(`Approval failed: ${response.statusText}`);
  }
}

export async function rejectEmail(emailId: string): Promise<void> {
  const response = await fetch(
    `${BASE_URL}/email-approvals/${encodeURIComponent(emailId)}/reject`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
    },
  );

  if (!response.ok) {
    throw new Error(`Rejection failed: ${response.statusText}`);
  }
}
