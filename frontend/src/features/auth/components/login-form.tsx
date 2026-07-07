"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LogIn, LoaderCircle } from "lucide-react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { ErrorAlert } from "@/components/ui/feedback";
import { InputField } from "@/components/ui/form-field";
import { login } from "@/lib/api/auth";
import { ApiClientError } from "@/lib/api/client";
import { authenticated } from "@/store/auth-slice";
import { useAppDispatch } from "@/store/hooks";

import { loginSchema, type LoginValues } from "../validation";

export function LoginForm() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const queryClient = useQueryClient();
  const form = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: "", password: "" } });
  const mutation = useMutation({
    mutationFn: login,
    onSuccess: ({ user }) => {
      dispatch(authenticated(user));
      queryClient.setQueryData(["auth", "me"], user);
      router.replace("/cases");
    },
  });

  return (
    <form className="space-y-5" onSubmit={form.handleSubmit((values) => mutation.mutate(values))} noValidate>
      {mutation.error ? <ErrorAlert message={mutation.error instanceof ApiClientError ? mutation.error.message : "Login failed."} /> : null}
      <InputField label="Email" type="email" autoComplete="email" placeholder="you@company.com" error={form.formState.errors.email?.message} {...form.register("email")} />
      <InputField label="Password" type="password" autoComplete="current-password" error={form.formState.errors.password?.message} {...form.register("password")} />
      <Button type="submit" className="w-full" disabled={mutation.isPending}>
        {mutation.isPending ? <LoaderCircle className="size-4 animate-spin" /> : <LogIn className="size-4" />}
        {mutation.isPending ? "Signing in..." : "Sign in"}
      </Button>
    </form>
  );
}
