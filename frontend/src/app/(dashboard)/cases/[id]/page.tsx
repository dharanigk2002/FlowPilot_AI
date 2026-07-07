import { CaseDetail } from "@/features/cases/components/case-detail";

export default async function CaseDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <CaseDetail caseId={Number(id)} />;
}
