import Link from "next/link";

import { AuthFrame } from "@/features/auth/components/auth-frame";
import { LoginForm } from "@/features/auth/components/login-form";

export default function LoginPage() {
  return <AuthFrame title="Welcome back" description="Sign in to manage cases and search company policy." footer={<>Need an account? <Link className="font-semibold text-indigo-600 hover:text-indigo-700" href="/register">Create one</Link></>}><LoginForm /></AuthFrame>;
}
