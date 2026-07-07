"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";

import { EmptyState, ErrorAlert, LoadingState } from "@/components/ui/feedback";
import { Pagination } from "@/components/ui/pagination";
import type { CasePriority, CaseStatus } from "@/lib/api/types";
import { cn, formatDate } from "@/lib/utils";

import { getCases } from "../api";

const priorityStyle: Record<CasePriority, string> = {
  LOW: "bg-slate-100 text-slate-700",
  MEDIUM: "bg-blue-50 text-blue-700",
  HIGH: "bg-amber-50 text-amber-700",
  URGENT: "bg-rose-50 text-rose-700",
};

const statusStyle: Record<CaseStatus, string> = {
  OPEN: "bg-emerald-50 text-emerald-700",
  IN_PROGRESS: "bg-blue-50 text-blue-700",
  PENDING_MANAGER_REVIEW: "bg-violet-50 text-violet-700",
  RESOLVED: "bg-slate-100 text-slate-700",
  CLOSED: "bg-slate-200 text-slate-700",
};

export function CaseList() {
  const [page, setPage] = useState(0);
  const query = useQuery({
    queryKey: ["cases", page],
    queryFn: () => getCases(page),
  });

  if (query.isPending) return <LoadingState label="Loading cases" />;
  if (query.isError) {
    return <ErrorAlert message="Cases could not be loaded. Check that the backend is running." />;
  }
  if (query.data.items.length === 0) {
    return (
      <EmptyState
        title="No cases yet"
        description="Create the first support case to begin the operations workflow."
      />
    );
  }

  return (
    <div className="space-y-5">
      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-5 py-3 font-semibold">Case</th>
                <th className="px-5 py-3 font-semibold">Customer</th>
                <th className="px-5 py-3 font-semibold">Priority</th>
                <th className="px-5 py-3 font-semibold">Status</th>
                <th className="px-5 py-3 font-semibold">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {query.data.items.map((supportCase) => (
                <tr key={supportCase.id} className="hover:bg-slate-50">
                  <td className="px-5 py-4">
                    <Link
                      href={`/cases/${supportCase.id}`}
                      className="font-semibold text-slate-900 hover:text-indigo-700"
                    >
                      {supportCase.title}
                    </Link>
                    <p className="mt-1 max-w-md truncate text-xs text-slate-500">
                      #{supportCase.id} · {supportCase.orderReference ?? "No order reference"}
                    </p>
                  </td>
                  <td className="px-5 py-4">
                    <p className="text-slate-800">{supportCase.customerName}</p>
                    <p className="text-xs text-slate-500">{formatLabel(supportCase.customerTier)}</p>
                  </td>
                  <td className="px-5 py-4">
                    <span className={cn("rounded-full px-2.5 py-1 text-xs font-semibold", priorityStyle[supportCase.priority])}>
                      {formatLabel(supportCase.priority)}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <span className={cn("rounded-full px-2.5 py-1 text-xs font-semibold", statusStyle[supportCase.status])}>
                      {formatLabel(supportCase.status)}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-5 py-4 text-slate-500">
                    {formatDate(supportCase.createdAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <Pagination page={query.data.page} totalPages={query.data.totalPages} onPageChange={setPage} />
    </div>
  );
}

function formatLabel(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
