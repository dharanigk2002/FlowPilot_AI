import { apiRequest } from "@/lib/api/client";
import type {
  AgentSuggestedAction,
  CaseStatus,
  CreateCaseInput,
  PageResponse,
  RagChatResponse,
  SubmitAgentRecommendationInput,
  SupportCase,
} from "@/lib/api/types";

export function getCases(page: number, size = 20) {
  return apiRequest<PageResponse<SupportCase>>(`/api/cases?page=${page}&size=${size}`);
}

export function getCase(caseId: number) {
  return apiRequest<SupportCase>(`/api/cases/${caseId}`);
}

export function createCase(input: CreateCaseInput) {
  return apiRequest<SupportCase>("/api/cases", { method: "POST", body: JSON.stringify(input) });
}

export function updateCaseStatus(caseId: number, status: CaseStatus) {
  return apiRequest<SupportCase>(`/api/cases/${caseId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

export function generateCaseRecommendation(caseId: number) {
  return apiRequest<RagChatResponse>(`/api/cases/${caseId}/recommendation`, {
    method: "POST",
  });
}

export function submitAgentRecommendation(
  caseId: number,
  input: SubmitAgentRecommendationInput,
) {
  return apiRequest<SupportCase>(`/api/cases/${caseId}/agent-recommendation`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export const agentSuggestedActions: Array<{
  value: AgentSuggestedAction;
  label: string;
}> = [
  { value: "REFUND", label: "Refund" },
  { value: "COMPENSATION", label: "Compensation" },
  { value: "REPLACEMENT", label: "Replacement" },
  { value: "REJECT_REQUEST", label: "Reject request" },
  { value: "ESCALATE", label: "Escalate" },
];
