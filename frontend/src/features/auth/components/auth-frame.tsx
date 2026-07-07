import { ShieldCheck } from "lucide-react";
import type { ReactNode } from "react";

export function AuthFrame({ title, description, children, footer }: { title: string; description: string; children: ReactNode; footer: ReactNode }) {
  return (
    <main className="grid min-h-screen bg-slate-950 lg:grid-cols-[1.05fr_0.95fr]">
      <section className="hidden flex-col justify-between overflow-hidden p-12 text-white lg:flex">
        <div className="flex items-center gap-3 text-lg font-bold"><span className="rounded-xl bg-indigo-500 p-2"><ShieldCheck className="size-5" /></span>FlowPilot AI</div>
        <div className="max-w-xl">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-indigo-300">Policy-grounded operations</p>
          <h1 className="mt-5 text-5xl font-bold leading-tight tracking-tight">Make support decisions with evidence, not guesswork.</h1>
          <p className="mt-6 text-lg leading-8 text-slate-300">Search company policies, manage customer cases, and keep every AI recommendation tied to a source.</p>
        </div>
        <p className="text-sm text-slate-500">Secure local AI workflow · Human approval by design</p>
      </section>
      <section className="flex items-center justify-center bg-slate-50 px-5 py-10 sm:px-10">
        <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl shadow-slate-950/5 sm:p-8">
          <div className="mb-7 lg:hidden"><span className="font-bold text-slate-950">FlowPilot AI</span></div>
          <h2 className="text-2xl font-bold tracking-tight text-slate-950">{title}</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
          <div className="mt-7">{children}</div>
          <div className="mt-6 border-t border-slate-200 pt-5 text-center text-sm text-slate-600">{footer}</div>
        </div>
      </section>
    </main>
  );
}
