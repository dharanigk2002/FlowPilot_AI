"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Bot, LoaderCircle, Send } from "lucide-react";
import Link from "next/link";
import { useState, type ReactNode } from "react";

import { Button } from "@/components/ui/button";
import { EmptyState, ErrorAlert, LoadingState } from "@/components/ui/feedback";
import type { AgentSuggestedAction, CasePriority, CaseStatus } from "@/lib/api/types";
import { cn, formatDate } from "@/lib/utils";
import { useAppSelector } from "@/store/hooks";

import {
  agentSuggestedActions,
  generateCaseRecommendation,
  getCase,
  submitAgentRecommendation,
  updateCaseStatus,
} from "../api";

const statusOptions: CaseStatus[] = [
  "OPEN",
  "IN_PROGRESS",
  "PENDING_MANAGER_REVIEW",
  "RESOLVED",
  "CLOSED",
];

const statusStyle: Record<CaseStatus, string> = {
  OPEN: "bg-emerald-50 text-emerald-700",
  IN_PROGRESS: "bg-blue-50 text-blue-700",
  PENDING_MANAGER_REVIEW: "bg-violet-50 text-violet-700",
  RESOLVED: "bg-slate-100 text-slate-700",
  CLOSED: "bg-slate-200 text-slate-700",
};

const priorityStyle: Record<CasePriority, string> = {
  LOW: "bg-slate-100 text-slate-700",
  MEDIUM: "bg-blue-50 text-blue-700",
  HIGH: "bg-amber-50 text-amber-700",
  URGENT: "bg-rose-50 text-rose-700",
};

export function CaseDetail({ caseId }: { caseId: number }) {
  const queryClient = useQueryClient();
  const user = useAppSelector((state) => state.auth.user);
  const [suggestedAction, setSuggestedAction] = useState<AgentSuggestedAction>("REPLACEMENT");
  const [agentNotes, setAgentNotes] = useState("");
  const query = useQuery({
    queryKey: ["cases", caseId],
    queryFn: () => getCase(caseId),
    enabled: Number.isFinite(caseId),
  });
  const statusMutation = useMutation({
    mutationFn: (status: CaseStatus) => updateCaseStatus(caseId, status),
    onSuccess: (updatedCase) => {
      queryClient.setQueryData(["cases", caseId], updatedCase);
      queryClient.invalidateQueries({ queryKey: ["cases"] });
    },
  });
  const aiRecommendationMutation = useMutation({
    mutationFn: () => generateCaseRecommendation(caseId),
  });
  const agentRecommendationMutation = useMutation({
    mutationFn: () =>
      submitAgentRecommendation(caseId, {
        suggestedAction,
        notes: agentNotes,
      }),
    onSuccess: (updatedCase) => {
      queryClient.setQueryData(["cases", caseId], updatedCase);
      queryClient.invalidateQueries({ queryKey: ["cases"] });
      setAgentNotes("");
    },
  });

  if (!Number.isFinite(caseId)) {
    return <EmptyState title="Invalid case" description="The case id in the URL is not valid." />;
  }
  if (query.isPending) return <LoadingState label="Loading case" />;
  if (query.isError) return <ErrorAlert message="Case could not be loaded." />;

  const supportCase = query.data;
  const canUpdateStatus = user?.role === "MANAGER" || user?.role === "ADMIN";
  const canSubmitAgentRecommendation = user?.role === "SUPPORT_AGENT";
  const notesAreValid = agentNotes.trim().length >= 10;
  const applyAiRecommendationToNotes = () => {
    const answer = aiRecommendationMutation.data?.answer.trim();
    if (!answer) return;

    setAgentNotes((currentNotes) => {
      const trimmedNotes = currentNotes.trim();
      if (!trimmedNotes) return answer;
      return `${trimmedNotes}\n\nAI recommendation:\n${answer}`;
    });
  };

  return (
    <div className="space-y-6">
      <Link href="/cases" className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-indigo-700">
        <ArrowLeft className="size-4" />
        Back to cases
      </Link>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p className="text-sm font-semibold text-indigo-600">Case #{supportCase.id}</p>
            <h1 className="mt-2 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">
              {supportCase.title}
            </h1>
            <p className="mt-3 max-w-3xl leading-7 text-slate-600">{supportCase.description}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Badge className={priorityStyle[supportCase.priority]}>{formatLabel(supportCase.priority)}</Badge>
            <Badge className={statusStyle[supportCase.status]}>{formatLabel(supportCase.status)}</Badge>
          </div>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="font-semibold text-slate-950">Case information</h2>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <DetailItem label="Customer" value={supportCase.customerName} />
              <DetailItem label="Customer email" value={supportCase.customerEmail ?? "Not provided"} />
              <DetailItem label="Customer tier" value={formatLabel(supportCase.customerTier)} />
              <DetailItem label="Order reference" value={supportCase.orderReference ?? "Not provided"} />
              <DetailItem label="Order value" value={formatCurrency(supportCase.orderValue)} />
              <DetailItem label="Created by" value={`${supportCase.createdBy.displayName} (${supportCase.createdBy.email})`} />
              <DetailItem label="Created" value={formatDate(supportCase.createdAt)} />
              <DetailItem label="Last updated" value={formatDate(supportCase.updatedAt)} />
            </div>
          </section>

          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="font-semibold text-slate-950">Agent recommendation</h2>
            <p className="mt-2 text-sm text-slate-500">
              Record the human recommendation and submit the case for manager review.
            </p>

            {supportCase.agentRecommendation ? (
              <div className="mt-5 rounded-xl border border-violet-100 bg-violet-50/70 p-4">
                <div className="flex flex-wrap items-center gap-2">
                  <Badge className="bg-violet-100 text-violet-700">
                    {formatLabel(supportCase.agentRecommendation.suggestedAction)}
                  </Badge>
                  <span className="text-xs text-slate-500">
                    Submitted {formatDate(supportCase.agentRecommendation.recommendedAt)}
                  </span>
                </div>
                <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">
                  {supportCase.agentRecommendation.notes}
                </p>
                {supportCase.agentRecommendation.recommendedBy ? (
                  <p className="mt-3 text-xs text-slate-500">
                    By {supportCase.agentRecommendation.recommendedBy.displayName} (
                    {supportCase.agentRecommendation.recommendedBy.email})
                  </p>
                ) : null}
              </div>
            ) : null}

            {canSubmitAgentRecommendation ? (
              <div className="mt-5 grid gap-4">
                <label className="grid gap-1.5 text-sm font-medium text-slate-700">
                  Suggested action
                  <select
                    className="min-h-10 rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-800 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-100"
                    value={suggestedAction}
                    onChange={(event) => setSuggestedAction(event.target.value as AgentSuggestedAction)}
                  >
                    {agentSuggestedActions.map((action) => (
                      <option key={action.value} value={action.value}>
                        {action.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="grid gap-1.5 text-sm font-medium text-slate-700">
                  Recommendation notes
                  <textarea
                    className="min-h-32 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-100"
                    placeholder="Example: Customer shared clear photos of the damaged product. Recommend replacement and submit for manager review."
                    value={agentNotes}
                    onChange={(event) => setAgentNotes(event.target.value)}
                  />
                </label>
                {agentRecommendationMutation.error ? (
                  <ErrorAlert message="Agent recommendation could not be submitted." />
                ) : null}
                <Button
                  className="justify-self-start"
                  onClick={() => agentRecommendationMutation.mutate()}
                  disabled={!notesAreValid || agentRecommendationMutation.isPending}
                >
                  {agentRecommendationMutation.isPending ? (
                    <LoaderCircle className="size-4 animate-spin" />
                  ) : (
                    <Send className="size-4" />
                  )}
                  Submit for manager review
                </Button>
                {!notesAreValid ? (
                  <p className="text-xs text-slate-500">Write at least 10 characters before submitting.</p>
                ) : null}
              </div>
            ) : null}
          </section>
        </div>

        <aside className="space-y-6">
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="font-semibold text-slate-950">Status</h2>
            <p className="mt-2 text-sm text-slate-500">
              Managers and admins can move a case through review and resolution.
            </p>
            <div className="mt-4">
              <select
                className="min-h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-800 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-100 disabled:cursor-not-allowed disabled:opacity-60"
                value={supportCase.status}
                onChange={(event) => statusMutation.mutate(event.target.value as CaseStatus)}
                disabled={!canUpdateStatus || statusMutation.isPending}
              >
                {statusOptions.map((status) => (
                  <option key={status} value={status}>
                    {formatLabel(status)}
                  </option>
                ))}
              </select>
              {statusMutation.isPending ? (
                <p className="mt-2 flex items-center gap-2 text-xs text-slate-500">
                  <LoaderCircle className="size-3.5 animate-spin" />
                  Updating status...
                </p>
              ) : null}
              {!canUpdateStatus ? (
                <p className="mt-2 text-xs text-slate-500">Only managers and admins can update status.</p>
              ) : null}
              {statusMutation.error ? (
                <p className="mt-2 text-xs text-rose-600">Status could not be updated.</p>
              ) : null}
            </div>
          </section>

          <section className="rounded-2xl border border-indigo-200 bg-indigo-50/70 p-6">
            <div className="flex items-start gap-3">
              <span className="rounded-lg bg-white p-2 text-indigo-600 shadow-sm">
                <Bot className="size-5" />
              </span>
              <div>
                <h2 className="font-semibold text-slate-950">AI recommendation</h2>
                <p className="mt-2 text-sm leading-6 text-slate-600">
                  Generate a policy-backed recommendation from this case and the uploaded documents.
                </p>
                <Button
                  className="mt-4 w-full"
                  onClick={() => aiRecommendationMutation.mutate()}
                  disabled={aiRecommendationMutation.isPending}
                >
                  {aiRecommendationMutation.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <Bot className="size-4" />}
                  {aiRecommendationMutation.isPending ? "Generating..." : "Generate recommendation"}
                </Button>
              </div>
            </div>
            {aiRecommendationMutation.error ? (
              <div className="mt-4">
                <ErrorAlert message="AI recommendation could not be generated. Check that policy documents and the AI model are available." />
              </div>
            ) : null}
            {aiRecommendationMutation.data ? (
              <div className="mt-5 rounded-xl border border-indigo-100 bg-white p-4">
                <p className="whitespace-pre-wrap text-sm leading-6 text-slate-700">
                  {aiRecommendationMutation.data.answer}
                </p>
                {canSubmitAgentRecommendation ? (
                  <Button
                    variant="secondary"
                    className="mt-4 w-full"
                    onClick={applyAiRecommendationToNotes}
                    disabled={!aiRecommendationMutation.data.answer.trim()}
                  >
                    Use as agent notes
                  </Button>
                ) : null}
                {aiRecommendationMutation.data.citations.length > 0 ? (
                  <div className="mt-4 space-y-2">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Sources</p>
                    {aiRecommendationMutation.data.citations.map((citation) => (
                      <div key={`${citation.sourceNumber}-${citation.documentId}-${citation.chunkIndex}`} className="rounded-lg bg-slate-50 p-3 text-xs text-slate-600">
                        <span className="font-semibold text-slate-800">Source {citation.sourceNumber}</span>
                        {" · "}
                        {citation.fileName}
                        {citation.chunkIndex !== null ? ` · chunk ${citation.chunkIndex}` : ""}
                      </div>
                    ))}
                  </div>
                ) : null}
              </div>
            ) : null}
          </section>
        </aside>
      </div>
    </div>
  );
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-slate-100 bg-slate-50/70 p-4">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 break-words text-sm font-medium text-slate-900">{value}</p>
    </div>
  );
}

function Badge({ className, children }: { className: string; children: ReactNode }) {
  return <span className={cn("rounded-full px-3 py-1 text-xs font-semibold", className)}>{children}</span>;
}

function formatLabel(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatCurrency(value: number | null) {
  if (value === null) return "Not provided";
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(value);
}
