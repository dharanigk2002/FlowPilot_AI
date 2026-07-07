export type UserRole = "ADMIN" | "MANAGER" | "SUPPORT_AGENT";

export interface CurrentUser {
  id: number;
  email: string;
  displayName: string;
  role: UserRole;
}

export interface AuthResponse {
  expiresInSeconds: number;
  user: CurrentUser;
}

export interface FieldErrorDetail {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: FieldErrorDetail[];
}

export type CasePriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type CaseStatus =
  | "OPEN"
  | "IN_PROGRESS"
  | "PENDING_MANAGER_REVIEW"
  | "RESOLVED"
  | "CLOSED";
export type CustomerTier = "STANDARD" | "PREMIUM";

export interface SupportCase {
  id: number;
  title: string;
  description: string;
  customerName: string;
  customerEmail: string | null;
  customerTier: CustomerTier;
  orderReference: string | null;
  orderValue: number | null;
  priority: CasePriority;
  status: CaseStatus;
  createdBy: Pick<CurrentUser, "id" | "email" | "displayName">;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCaseInput {
  title: string;
  description: string;
  customerName: string;
  customerEmail: string | null;
  customerTier: CustomerTier;
  orderReference: string | null;
  orderValue: number | null;
  priority: CasePriority;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type DocumentStatus = "PROCESSING" | "READY" | "FAILED";

export interface KnowledgeDocument {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  status: DocumentStatus;
  chunkCount: number;
  errorMessage: string | null;
  uploadedBy: Pick<CurrentUser, "id" | "email" | "displayName">;
  createdAt: string;
  updatedAt: string;
}

export interface RagCitation {
  sourceNumber: number;
  documentId: string;
  fileName: string;
  chunkIndex: number | null;
  score: number | null;
}

export interface RagChatResponse {
  answer: string;
  citations: RagCitation[];
  insufficientEvidence: boolean;
}
