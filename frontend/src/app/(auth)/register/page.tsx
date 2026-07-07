import Link from "next/link";

import { AuthFrame } from "@/features/auth/components/auth-frame";
import { RegisterForm } from "@/features/auth/components/register-form";

export default function RegisterPage() {
  return <AuthFrame title="Create your workspace account" description="Start as a support agent and explore the FlowPilot workflow." footer={<>Already registered? <Link className="font-semibold text-indigo-600 hover:text-indigo-700" href="/login">Sign in</Link></>}><RegisterForm /></AuthFrame>;
}
