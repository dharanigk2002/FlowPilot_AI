import { PageHeader } from "@/components/ui/page-header";
import { CreateCaseForm } from "@/features/cases/components/create-case-form";

export default function NewCasePage() {
  return <div className="mx-auto max-w-3xl space-y-7"><PageHeader eyebrow="Cases" title="Create support case" description="Capture the customer context accurately so policy retrieval and later AI analysis have reliable inputs." /><CreateCaseForm /></div>;
}
