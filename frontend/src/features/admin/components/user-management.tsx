"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { LoaderCircle, ShieldAlert, UsersRound } from "lucide-react";
import { useState } from "react";

import { ErrorAlert, LoadingState } from "@/components/ui/feedback";
import { Pagination } from "@/components/ui/pagination";
import { cn } from "@/lib/utils";
import type { ManagedUser, UserRole } from "@/lib/api/types";
import { useAppSelector } from "@/store/hooks";

import { getUsers, updateUserRole } from "../api";

const roles: Array<{ value: UserRole; label: string }> = [
  { value: "SUPPORT_AGENT", label: "Support agent" },
  { value: "MANAGER", label: "Manager" },
  { value: "ADMIN", label: "Administrator" },
];

const roleStyles: Record<UserRole, string> = {
  ADMIN: "bg-indigo-50 text-indigo-700",
  MANAGER: "bg-violet-50 text-violet-700",
  SUPPORT_AGENT: "bg-emerald-50 text-emerald-700",
};

export function UserManagement() {
  const [page, setPage] = useState(0);
  const currentUser = useAppSelector((state) => state.auth.user);
  const queryClient = useQueryClient();
  const users = useQuery({
    queryKey: ["users", page],
    queryFn: () => getUsers(page),
  });
  const roleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: UserRole }) =>
      updateUserRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
      queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });

  if (currentUser?.role !== "ADMIN") {
    return (
      <section className="rounded-xl border border-amber-200 bg-amber-50 p-6 text-sm text-amber-900">
        <div className="flex items-start gap-3">
          <ShieldAlert className="mt-0.5 size-5 shrink-0" />
          <div>
            <h2 className="font-semibold">Admin access required</h2>
            <p className="mt-1">Only administrators can manage workspace users and roles.</p>
          </div>
        </div>
      </section>
    );
  }

  if (users.isPending) return <LoadingState label="Loading users" />;
  if (users.isError) return <ErrorAlert message="Users could not be loaded." />;

  return (
    <section className="space-y-5">
      {roleMutation.error ? <ErrorAlert message="Role could not be updated." /> : null}
      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center gap-3 border-b border-slate-200 px-5 py-4">
          <span className="rounded-lg bg-indigo-50 p-2 text-indigo-600">
            <UsersRound className="size-5" />
          </span>
          <div>
            <h2 className="font-semibold text-slate-950">Workspace users</h2>
            <p className="text-sm text-slate-500">Assign elevated permissions only when needed.</p>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-5 py-3">User</th>
                <th className="px-5 py-3">Current role</th>
                <th className="px-5 py-3">Change role</th>
                <th className="px-5 py-3">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {users.data.items.map((user) => (
                <UserRow
                  key={user.id}
                  user={user}
                  isCurrentUser={user.id === currentUser.id}
                  isUpdating={roleMutation.isPending && roleMutation.variables?.userId === user.id}
                  onRoleChange={(role) => roleMutation.mutate({ userId: user.id, role })}
                />
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <Pagination page={users.data.page} totalPages={users.data.totalPages} onPageChange={setPage} />
    </section>
  );
}

function UserRow({
  user,
  isCurrentUser,
  isUpdating,
  onRoleChange,
}: {
  user: ManagedUser;
  isCurrentUser: boolean;
  isUpdating: boolean;
  onRoleChange: (role: UserRole) => void;
}) {
  return (
    <tr className="align-middle">
      <td className="px-5 py-4">
        <div className="font-medium text-slate-950">{user.displayName}</div>
        <div className="text-xs text-slate-500">{user.email}</div>
      </td>
      <td className="px-5 py-4">
        <span className={cn("rounded-full px-2.5 py-1 text-xs font-semibold", roleStyles[user.role])}>
          {formatRole(user.role)}
        </span>
      </td>
      <td className="px-5 py-4">
        <div className="flex items-center gap-2">
          <select
            className="min-h-10 rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-800 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-100 disabled:cursor-not-allowed disabled:opacity-60"
            value={user.role}
            onChange={(event) => onRoleChange(event.target.value as UserRole)}
            disabled={isUpdating}
            aria-label={`Change role for ${user.displayName}`}
          >
            {roles.map((role) => (
              <option key={role.value} value={role.value}>
                {role.label}
              </option>
            ))}
          </select>
          {isUpdating ? <LoaderCircle className="size-4 animate-spin text-slate-400" /> : null}
          {isCurrentUser ? <span className="text-xs text-slate-400">You</span> : null}
        </div>
      </td>
      <td className="px-5 py-4 text-slate-500">{formatDate(user.createdAt)}</td>
    </tr>
  );
}

function formatRole(role: UserRole) {
  return role
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(new Date(value));
}
