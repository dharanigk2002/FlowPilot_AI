import { PageHeader } from "@/components/ui/page-header";
import { UserManagement } from "@/features/admin/components/user-management";

export default function AdminUsersPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Administration"
        title="User roles"
        description="Manage workspace access by assigning support agent, manager, and administrator roles."
      />
      <UserManagement />
    </div>
  );
}
