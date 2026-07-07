import { apiRequest } from "@/lib/api/client";
import type { KnowledgeDocument, PageResponse } from "@/lib/api/types";

export function getDocuments(page: number, size = 20) {
  return apiRequest<PageResponse<KnowledgeDocument>>(`/api/documents?page=${page}&size=${size}`);
}

export function uploadDocument(file: File) {
  const body = new FormData();
  body.append("file", file);
  return apiRequest<KnowledgeDocument>("/api/documents", { method: "POST", body });
}
