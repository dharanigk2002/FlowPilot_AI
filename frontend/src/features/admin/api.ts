import { apiRequest } from "@/lib/api/client";
import type { ManagedUser, PageResponse, UserRole } from "@/lib/api/types";

export function getUsers(page: number, size = 20) {
  return apiRequest<PageResponse<ManagedUser>>(`/api/users?page=${page}&size=${size}`);
}

export function updateUserRole(userId: number, role: UserRole) {
  return apiRequest<ManagedUser>(`/api/users/${userId}/role`, {
    method: "PATCH",
    body: JSON.stringify({ role }),
  });
}
