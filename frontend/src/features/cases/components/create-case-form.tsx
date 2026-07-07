"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LoaderCircle, Save } from "lucide-react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { ErrorAlert } from "@/components/ui/feedback";
import { InputField, SelectField, TextareaField } from "@/components/ui/form-field";
import { ApiClientError } from "@/lib/api/client";

import { createCase } from "../api";
import { createCaseSchema, type CreateCaseValues } from "../validation";

export function CreateCaseForm() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const form = useForm<CreateCaseValues>({ resolver: zodResolver(createCaseSchema), defaultValues: { title: "", description: "", customerName: "", customerEmail: "", customerTier: "STANDARD", orderReference: "", orderValue: "", priority: "MEDIUM" } });
  const mutation = useMutation({
    mutationFn: (values: CreateCaseValues) => createCase({ ...values, customerEmail: values.customerEmail || null, orderReference: values.orderReference || null, orderValue: values.orderValue ? Number(values.orderValue) : null }),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ["cases"] }); router.push("/cases"); },
  });

  return (
    <form className="space-y-6 rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7" onSubmit={form.handleSubmit((values) => mutation.mutate(values))} noValidate>
      {mutation.error ? <ErrorAlert message={mutation.error instanceof ApiClientError ? mutation.error.message : "The case could not be created."} /> : null}
      <div className="grid gap-5 sm:grid-cols-2"><div className="sm:col-span-2"><InputField label="Case title" placeholder="Premium delivery delayed" error={form.formState.errors.title?.message} {...form.register("title")} /></div><div className="sm:col-span-2"><TextareaField label="Issue description" placeholder="Describe the customer issue and requested outcome." error={form.formState.errors.description?.message} {...form.register("description")} /></div><InputField label="Customer name" error={form.formState.errors.customerName?.message} {...form.register("customerName")} /><InputField label="Customer email" type="email" error={form.formState.errors.customerEmail?.message} {...form.register("customerEmail")} /><SelectField label="Customer tier" error={form.formState.errors.customerTier?.message} {...form.register("customerTier")}><option value="STANDARD">Standard</option><option value="PREMIUM">Premium</option></SelectField><SelectField label="Priority" error={form.formState.errors.priority?.message} {...form.register("priority")}><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option><option value="URGENT">Urgent</option></SelectField><InputField label="Order reference" error={form.formState.errors.orderReference?.message} {...form.register("orderReference")} /><InputField label="Order value" inputMode="decimal" placeholder="85000.00" error={form.formState.errors.orderValue?.message} {...form.register("orderValue")} /></div>
      <div className="flex justify-end gap-3 border-t border-slate-200 pt-5"><Button variant="secondary" onClick={() => router.back()}>Cancel</Button><Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <Save className="size-4" />}{mutation.isPending ? "Creating..." : "Create case"}</Button></div>
    </form>
  );
}
