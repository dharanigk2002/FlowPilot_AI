import { apiRequest } from "@/lib/api/client";
import type { CreateCaseInput, PageResponse, SupportCase } from "@/lib/api/types";

export function getCases(page: number, size = 20) {
  return apiRequest<PageResponse<SupportCase>>(`/api/cases?page=${page}&size=${size}`);
}

export function createCase(input: CreateCaseInput) {
  return apiRequest<SupportCase>("/api/cases", { method: "POST", body: JSON.stringify(input) });
}
