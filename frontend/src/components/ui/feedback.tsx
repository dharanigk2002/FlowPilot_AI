import { AlertCircle, Inbox, LoaderCircle } from "lucide-react";
import type { ReactNode } from "react";

export function LoadingState({ label = "Loading" }: { label?: string }) {
  return <div className="flex min-h-48 items-center justify-center gap-3 text-sm text-slate-500"><LoaderCircle className="size-5 animate-spin" aria-hidden="true" /><span>{label}...</span></div>;
}

export function ErrorAlert({ message }: { message: string }) {
  return <div className="flex gap-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800" role="alert"><AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /><span>{message}</span></div>;
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <div className="flex min-h-64 flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 bg-slate-50/70 p-8 text-center"><span className="mb-4 rounded-full bg-white p-3 text-slate-400 shadow-sm"><Inbox className="size-6" aria-hidden="true" /></span><h2 className="font-semibold text-slate-900">{title}</h2><p className="mt-1 max-w-md text-sm text-slate-500">{description}</p>{action ? <div className="mt-5">{action}</div> : null}</div>;
}
