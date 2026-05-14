import { FolderTree } from "lucide-react";
import { Badge } from "../../components/ui/Badge";
import { EmptyState } from "../../components/ui/EmptyState";
import { AdminPageShell, AdminSurface } from "./components/AdminPageShell";

export function AdminCategoriesPage() {
  return (
    <AdminPageShell
      title="Categories"
      description="Category administration is intentionally staged behind a placeholder route so the admin navigation is complete without enabling CRUD yet."
      badges={
        <>
          <Badge variant="warning">Coming next</Badge>
          <Badge>Phase 2</Badge>
        </>
      }
    >
      <AdminSurface
        title="Planned scope"
        description="The next phase can add category workflows once CRUD requirements are finalized."
      >
          <EmptyState
            icon={<FolderTree className="size-14" />}
            title="Category administration is not enabled yet"
            description="Use the rest of the admin control panel for users, roles, permissions, and system operations while category work remains queued."
            className="py-10"
          />
      </AdminSurface>
    </AdminPageShell>
  );
}

