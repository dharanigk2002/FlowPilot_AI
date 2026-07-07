"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Bot,
  BriefcaseBusiness,
  FileText,
  LogOut,
  ShieldCheck,
} from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { LoadingState } from "@/components/ui/feedback";
import { logout } from "@/lib/api/auth";
import { cn } from "@/lib/utils";
import { unauthenticated } from "@/store/auth-slice";
import { useAppDispatch, useAppSelector } from "@/store/hooks";

const navigation = [
  { href: "/cases", label: "Cases", icon: BriefcaseBusiness },
  { href: "/knowledge", label: "Knowledge", icon: FileText },
  { href: "/assistant", label: "Assistant", icon: Bot },
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const queryClient = useQueryClient();
  const dispatch = useAppDispatch();
  const { user, status } = useAppSelector((state) => state.auth);
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      dispatch(unauthenticated());
      queryClient.clear();
      router.replace("/login");
    },
  });

  useEffect(() => {
    if (status === "unauthenticated") router.replace("/login");
  }, [router, status]);

  if (status !== "authenticated" || !user) {
    return (
      <main className="min-h-screen bg-slate-50">
        <LoadingState label="Checking your session" />
      </main>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 lg:grid lg:grid-cols-[250px_1fr]">
      <aside className="hidden min-h-screen border-r border-slate-800 bg-slate-950 px-4 py-6 text-white lg:flex lg:flex-col">
        <Link
          href="/cases"
          className="flex items-center gap-3 px-2 text-lg font-bold"
        >
          <span className="rounded-lg bg-indigo-500 p-2">
            <ShieldCheck className="size-4" />
          </span>
          FlowPilot AI
        </Link>
        <nav className="mt-10 space-y-1" aria-label="Main navigation">
          {navigation.map((item) => {
            const active =
              pathname === item.href || pathname.startsWith(`${item.href}/`);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition",
                  active
                    ? "bg-indigo-500 text-white"
                    : "text-slate-300 hover:bg-slate-900 hover:text-white",
                )}
              >
                <item.icon className="size-4" />
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="mt-auto border-t border-slate-800 pt-5">
          <p className="truncate px-2 text-sm font-semibold">
            {user.displayName}
          </p>
          <p className="truncate px-2 text-xs text-slate-400">
            {user.role.replaceAll("_", " ")}
          </p>
          <button
            className="mt-4 flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-slate-300 hover:bg-slate-900 hover:text-white"
            onClick={() => logoutMutation.mutate()}
            disabled={logoutMutation.isPending}
          >
            <LogOut className="size-4" />
            Sign out
          </button>
        </div>
      </aside>
      <div className="min-w-0">
        <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/95 px-4 py-3 backdrop-blur lg:hidden">
          <div className="flex items-center justify-between">
            <span className="font-bold text-slate-950">FlowPilot AI</span>
            <button
              className="rounded-lg p-2 text-slate-600 hover:bg-slate-100"
              onClick={() => logoutMutation.mutate()}
              aria-label="Sign out"
            >
              <LogOut className="size-5" />
            </button>
          </div>
          <nav
            className="mt-3 flex gap-1 overflow-x-auto"
            aria-label="Mobile navigation"
          >
            {navigation.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex shrink-0 items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium",
                  pathname.startsWith(item.href)
                    ? "bg-indigo-50 text-indigo-700"
                    : "text-slate-600",
                )}
              >
                <item.icon className="size-4" />
                {item.label}
              </Link>
            ))}
          </nav>
        </header>
        <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-10 lg:py-10">
          {children}
        </main>
      </div>
    </div>
  );
}
