import { PageHeader } from "@/components/ui/page-header";
import { AssistantPanel } from "@/features/assistant/components/assistant-panel";

export default function AssistantPage() {
  return <div className="space-y-7"><PageHeader eyebrow="RAG assistant" title="Ask FlowPilot" description="Get concise answers grounded in indexed company policy, with source metadata for every retrieval." /><AssistantPanel /></div>;
}
