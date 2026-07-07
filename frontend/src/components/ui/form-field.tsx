import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

function FieldShell({ label, htmlFor, error, hint, children }: { label: string; htmlFor: string; error?: string; hint?: string; children: ReactNode }) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={htmlFor} className="block text-sm font-medium text-slate-700">{label}</label>
      {children}
      {error ? <p className="text-sm text-rose-600" role="alert">{error}</p> : hint ? <p className="text-xs text-slate-500">{hint}</p> : null}
    </div>
  );
}

const controlClass = "w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 disabled:bg-slate-100";

export function InputField({ label, error, hint, className, id, ...props }: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string; hint?: string }) {
  const inputId = id ?? props.name;
  if (!inputId) throw new Error("InputField requires an id or name.");
  return <FieldShell label={label} htmlFor={inputId} error={error} hint={hint}><input id={inputId} className={cn(controlClass, className)} {...props} /></FieldShell>;
}

export function TextareaField({ label, error, hint, className, id, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement> & { label: string; error?: string; hint?: string }) {
  const inputId = id ?? props.name;
  if (!inputId) throw new Error("TextareaField requires an id or name.");
  return <FieldShell label={label} htmlFor={inputId} error={error} hint={hint}><textarea id={inputId} className={cn(controlClass, "min-h-28 resize-y", className)} {...props} /></FieldShell>;
}

export function SelectField({ label, error, children, className, id, ...props }: SelectHTMLAttributes<HTMLSelectElement> & { label: string; error?: string; children: ReactNode }) {
  const inputId = id ?? props.name;
  if (!inputId) throw new Error("SelectField requires an id or name.");
  return <FieldShell label={label} htmlFor={inputId} error={error}><select id={inputId} className={cn(controlClass, className)} {...props}>{children}</select></FieldShell>;
}
