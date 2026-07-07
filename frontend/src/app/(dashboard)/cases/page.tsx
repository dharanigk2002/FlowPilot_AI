import { Plus } from "lucide-react";
import Link from "next/link";

import { PageHeader } from "@/components/ui/page-header";
import { CaseList } from "@/features/cases/components/case-list";

export default function CasesPage() {
  return <div className="space-y-7"><PageHeader eyebrow="Operations" title="Support cases" description="Track customer issues, priority, and the people responsible for each decision." action={<Link href="/cases/new" className="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg bg-indigo-600 px-4 text-sm font-semibold text-white shadow-sm hover:bg-indigo-700"><Plus className="size-4" />New case</Link>} /><CaseList /></div>;
}
