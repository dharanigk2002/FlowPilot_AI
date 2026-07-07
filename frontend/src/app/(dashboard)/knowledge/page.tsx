import { PageHeader } from "@/components/ui/page-header";
import { DocumentManager } from "@/features/knowledge/components/document-manager";

export default function KnowledgePage() {
  return <div className="space-y-7"><PageHeader eyebrow="Knowledge base" title="Company policies" description="Manage the documents that ground semantic search and AI-generated answers." /><DocumentManager /></div>;
}
