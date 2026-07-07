"use client";

import { useMutation } from "@tanstack/react-query";
import { Bot, FileText, LoaderCircle, Send, Sparkles } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { ErrorAlert } from "@/components/ui/feedback";
import { ApiClientError } from "@/lib/api/client";

import { askAssistant } from "../api";

const suggestions = ["When is a customer eligible for a delivery-fee refund?", "Who must approve a product-price refund?", "Can a goodwill coupon and delivery-fee refund be combined?"];

export function AssistantPanel() {
  const [question, setQuestion] = useState("");
  const mutation = useMutation({ mutationFn: askAssistant });
  const submit = () => { const value = question.trim(); if (value) mutation.mutate(value); };

  return (
    <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_340px]">
      <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-200 bg-slate-50 px-5 py-4"><div className="flex items-center gap-3"><span className="rounded-lg bg-indigo-600 p-2 text-white"><Bot className="size-5" /></span><div><h2 className="font-semibold text-slate-950">Policy assistant</h2><p className="text-xs text-slate-500">Answers use indexed company evidence only</p></div></div></div>
        <div className="min-h-[420px] p-5 sm:p-7">{mutation.data ? <div className="space-y-6"><div className="rounded-xl bg-indigo-50 p-5"><div className="mb-3 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-indigo-600"><Sparkles className="size-4" />Answer</div><p className="whitespace-pre-wrap text-sm leading-7 text-slate-800">{mutation.data.answer}</p>{mutation.data.insufficientEvidence ? <p className="mt-4 text-xs font-medium text-amber-700">The indexed policies did not contain enough evidence for a grounded answer.</p> : null}</div>{mutation.data.citations.length > 0 ? <div><h3 className="mb-3 text-sm font-semibold text-slate-900">Sources</h3><div className="grid gap-3 sm:grid-cols-2">{mutation.data.citations.map((citation) => <article key={`${citation.sourceNumber}-${citation.documentId}-${citation.chunkIndex}`} className="rounded-lg border border-slate-200 p-4"><div className="flex items-center gap-2 text-xs font-semibold text-indigo-600"><FileText className="size-4" />Source {citation.sourceNumber}</div><p className="mt-2 break-words text-sm font-medium text-slate-800">{citation.fileName}</p><p className="mt-1 text-xs text-slate-500">Chunk {citation.chunkIndex ?? "—"}{citation.score == null ? "" : ` · ${(citation.score * 100).toFixed(1)}% match`}</p></article>)}</div></div> : null}</div> : <div className="flex min-h-[360px] flex-col items-center justify-center text-center"><span className="rounded-full bg-indigo-50 p-4 text-indigo-600"><Bot className="size-7" /></span><h2 className="mt-5 font-semibold text-slate-900">Ask about company policy</h2><p className="mt-2 max-w-md text-sm leading-6 text-slate-500">FlowPilot retrieves semantically relevant chunks and asks the local chat model to answer with citations.</p></div>}</div>
        <div className="border-t border-slate-200 bg-white p-4"><div className="flex items-end gap-3"><textarea value={question} onChange={(event) => setQuestion(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); submit(); } }} rows={2} maxLength={2000} placeholder="Ask a policy question..." className="min-h-12 flex-1 resize-none rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100" /><Button className="h-12 px-4" disabled={!question.trim() || mutation.isPending} onClick={submit} aria-label="Ask assistant">{mutation.isPending ? <LoaderCircle className="size-5 animate-spin" /> : <Send className="size-5" />}</Button></div>{mutation.error ? <div className="mt-3"><ErrorAlert message={mutation.error instanceof ApiClientError ? mutation.error.message : "The assistant could not generate an answer."} /></div> : null}</div>
      </section>
      <aside className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="text-sm font-semibold text-slate-900">Try a grounded question</h2><p className="mt-1 text-xs leading-5 text-slate-500">Questions work best when they describe a policy decision, condition, or approval rule.</p><div className="mt-4 space-y-2">{suggestions.map((suggestion) => <button key={suggestion} onClick={() => setQuestion(suggestion)} className="w-full rounded-lg border border-slate-200 p-3 text-left text-sm leading-5 text-slate-700 hover:border-indigo-300 hover:bg-indigo-50">{suggestion}</button>)}</div></aside>
    </div>
  );
}
