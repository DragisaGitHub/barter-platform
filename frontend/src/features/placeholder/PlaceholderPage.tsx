import { EmptyState } from "../../components/ui/EmptyState";
import { Package } from "lucide-react";

interface PlaceholderPageProps {
  title: string;
  description?: string;
}

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <div className="max-w-7xl mx-auto">
      <h1 className="text-3xl font-bold text-slate-900 dark:text-white mb-8">{title}</h1>
      <EmptyState
        icon={<Package className="size-16" />}
        title="Coming Soon"
        description={description || "This feature is currently under development."}
      />
    </div>
  );
}
