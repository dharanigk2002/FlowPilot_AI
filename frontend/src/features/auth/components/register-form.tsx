"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LoaderCircle, UserPlus } from "lucide-react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { ErrorAlert } from "@/components/ui/feedback";
import { InputField, SelectField } from "@/components/ui/form-field";
import { register as registerUser } from "@/lib/api/auth";
import { ApiClientError } from "@/lib/api/client";
import { authenticated } from "@/store/auth-slice";
import { useAppDispatch } from "@/store/hooks";

import { registerSchema, type RegisterValues } from "../validation";

export function RegisterForm() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const queryClient = useQueryClient();
  const form = useForm<RegisterValues>({ resolver: zodResolver(registerSchema), defaultValues: { email: "", displayName: "", password: "", role: "SUPPORT_AGENT" } });
  const mutation = useMutation({
    mutationFn: registerUser,
    onSuccess: ({ user }) => {
      dispatch(authenticated(user));
      queryClient.setQueryData(["auth", "me"], user);
      router.replace("/cases");
    },
  });

  return (
    <form className="space-y-4" onSubmit={form.handleSubmit((values) => mutation.mutate(values))} noValidate>
      {mutation.error ? <ErrorAlert message={mutation.error instanceof ApiClientError ? mutation.error.message : "Registration failed."} /> : null}
      <InputField label="Display name" autoComplete="name" error={form.formState.errors.displayName?.message} {...form.register("displayName")} />
      <InputField label="Email" type="email" autoComplete="email" error={form.formState.errors.email?.message} {...form.register("email")} />
      <InputField label="Password" type="password" autoComplete="new-password" error={form.formState.errors.password?.message} {...form.register("password")} />
      <SelectField label="Demo role" error={form.formState.errors.role?.message} {...form.register("role")}>
        <option value="SUPPORT_AGENT">Support agent</option><option value="MANAGER">Manager</option><option value="ADMIN">Administrator</option>
      </SelectField>
      <p className="text-xs leading-5 text-amber-700">Role selection is enabled for the local demo only. Production accounts should be provisioned by an administrator.</p>
      <Button type="submit" className="w-full" disabled={mutation.isPending}>{mutation.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <UserPlus className="size-4" />}{mutation.isPending ? "Creating account..." : "Create account"}</Button>
    </form>
  );
}
