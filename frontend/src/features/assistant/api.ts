import { apiRequest } from "@/lib/api/client";
import type { RagChatResponse } from "@/lib/api/types";

export function askAssistant(question: string) {
  return apiRequest<RagChatResponse>("/api/ai/chat", { method: "POST", body: JSON.stringify({ question }) });
}
